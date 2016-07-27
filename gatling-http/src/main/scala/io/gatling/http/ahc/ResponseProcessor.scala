/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.ahc

import scala.util.control.NonFatal
import scala.collection.JavaConversions._

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.commons.util.StringHelper.Eol
import io.gatling.core.check.Check
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.http.HeaderNames
import io.gatling.http.action.sync.HttpTx
import io.gatling.http.check.{ HttpCheck, HttpCheckScope }
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.fetch.{ CssResourceFetched, RegularResourceFetched }
import io.gatling.http.referer.RefererHandling
import io.gatling.http.request.ExtraInfo
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper
import io.gatling.http.util.HttpHelper.{ isCss, resolveFromUri }
import io.gatling.http.util.HttpStringBuilder

import akka.actor.{ ActorRefFactory, Props }
import com.typesafe.scalalogging.StrictLogging
import org.asynchttpclient.Request
import org.asynchttpclient.uri.Uri
import org.asynchttpclient.util.HttpConstants.Methods._
import org.asynchttpclient.util.HttpConstants.ResponseStatusCodes._
import org.asynchttpclient.util.StringUtils.stringBuilder

class ResponseProcessor(statsEngine: StatsEngine, httpEngine: HttpEngine, configuration: GatlingConfiguration)(implicit actorRefFactory: ActorRefFactory) extends StrictLogging with NameGen {

  private def abort(tx: HttpTx, t: Throwable): Unit = {
    logger.error(s"ResponseProcessor crashed on session=${tx.session} request=${tx.request.requestName}: ${tx.request.ahcRequest} resourceFetcher=${tx.resourceFetcher} redirectCount=${tx.redirectCount}, forwarding user to the next action", t)
    tx.resourceFetcher match {
      case None                  => tx.next ! tx.session.markAsFailed
      case Some(resourceFetcher) => resourceFetcher ! RegularResourceFetched(tx.request.ahcRequest.getUri, KO, Session.Identity, tx.silent)
    }
  }

  def onCompleted(tx: HttpTx, response: Response): Unit =
    try {
      processResponse(tx, response)
    } catch {
      case NonFatal(t) => abort(tx, t)
    }

  def onThrowable(tx: HttpTx, response: Response, errorMessage: String): Unit =
    try {
      ko(tx, Session.Identity, response, errorMessage)
    } catch {
      case NonFatal(t) => abort(tx, t)
    }

  private def logRequest(
    tx:           HttpTx,
    status:       Status,
    response:     Response,
    errorMessage: Option[String] = None
  ): Unit =
    if (!tx.silent) {
      val fullRequestName = tx.fullRequestName
        def dump = {
          // hack: pre-cache url because it would reset the StringBuilder
          tx.request.ahcRequest.getUrl
          val buff = stringBuilder
          buff.append(Eol).append(">>>>>>>>>>>>>>>>>>>>>>>>>>").append(Eol)
          buff.append("Request:").append(Eol).append(s"$fullRequestName: $status ${errorMessage.getOrElse("")}").append(Eol)
          buff.append("=========================").append(Eol)
          buff.append("Session:").append(Eol).append(tx.session).append(Eol)
          buff.append("=========================").append(Eol)
          buff.append("HTTP request:").append(Eol).appendRequest(tx.request.ahcRequest, response.nettyRequest, configuration.core.charset)
          buff.append("=========================").append(Eol)
          buff.append("HTTP response:").append(Eol).appendResponse(response).append(Eol)
          buff.append("<<<<<<<<<<<<<<<<<<<<<<<<<")
          buff.toString
        }

      if (status == KO) {
        logger.warn(s"Request '$fullRequestName' failed: ${errorMessage.getOrElse("")}")
        if (!logger.underlying.isTraceEnabled) logger.debug(dump)
      }
      logger.debug(dump)

      val extraInfo: List[Any] = try {
        tx.request.config.extraInfoExtractor match {
          case None            => Nil
          case Some(extractor) => extractor(ExtraInfo(tx.request.requestName, status, tx.session, tx.request.ahcRequest, response))
        }
      } catch {
        case NonFatal(e) =>
          logger.warn("Encountered error while extracting extra request info", e)
          Nil
      }

      statsEngine.logResponse(
        tx.session,
        fullRequestName,
        response.timings,
        status,
        response.status.map(httpStatus => Integer.toString(httpStatus.getStatusCode)),
        errorMessage,
        extraInfo
      )
    }

  /**
   * This method is used to send a message to the data writer actor and then execute the next action
   *
   * @param tx the HTTP transaction
   * @param update the update to be applied on the Session
   * @param status the status of the request
   * @param response the response
   */
  private def executeNext(tx: HttpTx, update: Session => Session, status: Status, response: Response): Unit =
    tx.resourceFetcher match {
      case None =>
        val maybeResourceFetcherActor =
          if (status == KO)
            None
          else
            httpEngine.resourceFetcherActorForFetchedPage(tx.request.ahcRequest, response, tx)

        maybeResourceFetcherActor match {
          case Some(resourceFetcherActor) => actorRefFactory.actorOf(Props(resourceFetcherActor()), genName("resourceFetcher"))
          case None                       => tx.next ! tx.session.increaseDrift(nowMillis - response.timings.endTimestamp)
        }

      case Some(resourceFetcher) =>
        val uri = response.request.getUri

        if (isCss(response.headers)) {
          val httpProtocol = tx.request.config.httpComponents.httpProtocol
          resourceFetcher ! CssResourceFetched(uri, status, update, tx.silent, response.statusCode, response.lastModifiedOrEtag(httpProtocol), response.body.string)
        } else {
          resourceFetcher ! RegularResourceFetched(uri, status, update, tx.silent)
        }
    }

  private def logAndExecuteNext(tx: HttpTx, update: Session => Session, status: Status, response: Response, message: Option[String]): Unit = {

    val statusUpdate = status match {
      case KO if !tx.silent => Session.MarkAsFailedUpdate
      case _                => Session.Identity
    }
    val groupUpdate = logGroupRequestUpdate(tx, status, response.timings.responseTime)
    val totalUpdate = update andThen statusUpdate andThen groupUpdate

    val newTx = tx.copy(session = totalUpdate(tx.session))
    logRequest(newTx, status, response, message)
    // we pass update and not totalUpdate because it's only used for resources where updates are handled differently
    executeNext(newTx, update, status, response)
  }

  private def ko(tx: HttpTx, update: Session => Session, response: Response, message: String): Unit =
    logAndExecuteNext(tx, update, KO, response, Some(message))

  private def logGroupRequestUpdate(tx: HttpTx, status: Status, responseTimeInMillis: Int): Session => Session =
    if (tx.resourceFetcher.isEmpty && !tx.silent)
      // resource logging is done in ResourceFetcher
      _.logGroupRequest(responseTimeInMillis, status)
    else
      Session.Identity

  /**
   * This method processes the response if needed for each checks given by the user
   */
  private def processResponse(tx: HttpTx, response: Response): Unit = {

    import tx.request.config.httpComponents._

      def redirectRequest(statusCode: Int, redirectUri: Uri, sessionWithUpdatedCookies: Session): Request = {
        val originalRequest = tx.request.ahcRequest
        val originalMethod = originalRequest.getMethod

        val switchToGet = originalMethod != GET && (statusCode == MOVED_PERMANENTLY_301 || statusCode == SEE_OTHER_303 || (statusCode == FOUND_302 && !httpProtocol.responsePart.strict302Handling))
        val keepBody = statusCode == TEMPORARY_REDIRECT_307 || (statusCode == FOUND_302 && httpProtocol.responsePart.strict302Handling)

        val newHeaders = originalRequest.getHeaders
          .remove(HeaderNames.Host)
          .remove(HeaderNames.ContentLength)
          .remove(HeaderNames.Cookie)

        if (!keepBody)
          newHeaders.remove(HeaderNames.ContentType)

        val requestBuilder = new AhcRequestBuilder(if (switchToGet) GET else originalMethod, false)
          .setUri(redirectUri)
          .setCharset(configuration.core.charset)
          .setChannelPoolPartitioning(originalRequest.getChannelPoolPartitioning)
          .setAddress(originalRequest.getAddress)
          .setNameResolver(originalRequest.getNameResolver)
          .setVirtualHost(originalRequest.getVirtualHost)
          .setLocalAddress(originalRequest.getLocalAddress)
          .setProxyServer(originalRequest.getProxyServer)
          .setRealm(originalRequest.getRealm)
          .setHeaders(newHeaders)

        if (!httpProtocol.proxyPart.proxyExceptions.contains(redirectUri.getHost)) {
          val originalRequestProxy = if (originalRequest.getUri.getHost == redirectUri.getHost) Option(originalRequest.getProxyServer) else None
          val protocolProxy = httpProtocol.proxyPart.proxy
          originalRequestProxy.orElse(protocolProxy).foreach(requestBuilder.setProxyServer)
        }

        if (keepBody) {
          requestBuilder.setCharset(originalRequest.getCharset)
          if (originalRequest.getFormParams.nonEmpty)
            requestBuilder.setFormParams(originalRequest.getFormParams)
          Option(originalRequest.getStringData).foreach(requestBuilder.setBody)
          Option(originalRequest.getByteData).foreach(requestBuilder.setBody)
          Option(originalRequest.getCompositeByteData).foreach(requestBuilder.setBody)
          Option(originalRequest.getByteBufferData).foreach(requestBuilder.setBody)
          Option(originalRequest.getBodyGenerator).foreach(requestBuilder.setBody)
        }

        for (cookie <- CookieSupport.getStoredCookies(sessionWithUpdatedCookies, redirectUri))
          requestBuilder.addCookie(cookie)

        requestBuilder.build
      }

      def redirect(statusCode: Int, update: Session => Session): Unit =
        tx.request.config.maxRedirects match {
          case Some(maxRedirects) if maxRedirects == tx.redirectCount =>
            ko(tx, update, response, s"Too many redirects, max is $maxRedirects")

          case _ =>
            response.header(HeaderNames.Location) match {
              case Some(location) =>
                val redirectURI = resolveFromUri(tx.request.ahcRequest.getUri, location)

                val cacheRedirectUpdate =
                  if (httpProtocol.requestPart.cache)
                    cacheRedirect(tx.request.ahcRequest, redirectURI)
                  else
                    Session.Identity

                val groupUpdate = logGroupRequestUpdate(tx, OK, response.timings.responseTime)

                val totalUpdate = update andThen cacheRedirectUpdate andThen groupUpdate
                val newSession = totalUpdate(tx.session)

                val loggedTx = tx.copy(session = newSession, update = totalUpdate)
                logRequest(loggedTx, OK, response)

                val newAhcRequest = redirectRequest(statusCode, redirectURI, newSession)
                val redirectTx = loggedTx.copy(request = loggedTx.request.copy(ahcRequest = newAhcRequest), redirectCount = tx.redirectCount + 1)
                HttpTx.start(redirectTx)

              case None =>
                ko(tx, update, response, "Redirect status, yet no Location header")
            }
        }

      def cacheRedirect(originalRequest: Request, redirectUri: Uri): Session => Session =
        response.statusCode match {
          case Some(code) if HttpHelper.isPermanentRedirect(code) =>
            httpCaches.addRedirect(_, originalRequest, redirectUri)
          case _ => Session.Identity
        }

      def checkAndProceed(sessionUpdate: Session => Session, checks: List[HttpCheck]): Unit = {

        val (checkSaveUpdate, checkError) = Check.check(response, tx.session, checks)

        val status = checkError match {
          case None => OK
          case _    => KO
        }

        val cacheContentUpdate = httpCaches.cacheContent(httpProtocol, tx.request.ahcRequest, response)

        val totalUpdate = sessionUpdate andThen cacheContentUpdate andThen checkSaveUpdate

        logAndExecuteNext(tx, totalUpdate, status, response, checkError.map(_.message))
      }

    response.status match {

      case Some(status) =>
        val uri = tx.request.ahcRequest.getUri
        val storeCookiesUpdate: Session => Session =
          response.cookies match {
            case Nil     => Session.Identity
            case cookies => CookieSupport.storeCookies(_, uri, cookies)
          }
        val newUpdate = tx.update andThen storeCookiesUpdate
        val statusCode = status.getStatusCode

        if (HttpHelper.isRedirect(statusCode) && tx.request.config.followRedirect)
          redirect(statusCode, newUpdate)

        else {
          val checks =
            if (HttpHelper.isNotModified(statusCode))
              tx.request.config.checks.filter(c => c.scope != HttpCheckScope.Body && c.scope != HttpCheckScope.Checksum)
            else
              tx.request.config.checks

          val storeRefererUpdate =
            if (tx.resourceFetcher.isEmpty)
              RefererHandling.storeReferer(tx.request.ahcRequest, response, httpProtocol)
            else Session.Identity

          checkAndProceed(newUpdate andThen storeRefererUpdate, checks)
        }

      case None =>
        ko(tx, Session.Identity, response, "How come OnComplete was sent with no status?!")
    }
  }
}

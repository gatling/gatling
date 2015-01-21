/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.ahc

import com.ning.http.client.{ Request, RequestBuilder }
import com.ning.http.client.uri.Uri
import com.ning.http.util.StringUtils.stringBuilder

import akka.actor.{ ActorRef, Props }
import akka.actor.ActorDSL.actor
import akka.routing.RoundRobinPool
import io.gatling.core.akka.{ AkkaDefaults, BaseActor }
import io.gatling.core.check.Check
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.session.Session
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.util.StringHelper.Eol
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.http.HeaderNames
import io.gatling.http.action.HttpRequestAction
import io.gatling.http.cache.{ PermanentRedirect, CacheHandling }
import io.gatling.http.check.{ HttpCheck, HttpCheckScope }
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.fetch.{ CssResourceFetched, RegularResourceFetched, ResourceFetcher }
import io.gatling.http.referer.RefererHandling
import io.gatling.http.request.ExtraInfo
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper
import io.gatling.http.util.HttpHelper.{ isCss, resolveFromUri }
import io.gatling.http.util.HttpStringBuilder

object AsyncHandlerActor extends AkkaDefaults {

  private var _instance: Option[ActorRef] = None

  def start(): Unit =
    if (!_instance.isDefined) {
      _instance = Some(system.actorOf(RoundRobinPool(3 * Runtime.getRuntime.availableProcessors).props(Props[AsyncHandlerActor]), "asyncHandler"))
      system.registerOnTermination(_instance = None)
    }

  def instance() = _instance match {
    case Some(a) => a
    case _       => throw new UnsupportedOperationException("AsyncHandlerActor pool hasn't been started")
  }
}

class AsyncHandlerActor extends BaseActor with DataWriterClient {

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {

      def abort(tx: HttpTx): Unit = {
        logger.error(s"AsyncHandlerActor crashed on message $message, forwarding user to the next action", reason)
        if (tx.primary)
          tx.next ! tx.session.markAsFailed
        else {
          val uri = tx.request.ahcRequest.getUri
          tx.next ! RegularResourceFetched(uri, KO, Session.Identity, tx.silent)
        }
      }

    message match {
      case Some(OnCompleted(tx, _)) =>
        abort(tx)

      case Some(OnThrowable(tx, _, _)) =>
        abort(tx)

      case _ =>
        logger.error(s"AsyncHandlerActor crashed on unknown message $message, dropping")
    }
  }

  def receive = {
    case OnCompleted(tx, response)               => processResponse(tx, response)
    case OnThrowable(tx, response, errorMessage) => ko(tx, Session.Identity, response, errorMessage)
  }

  private def logRequest(
    tx: HttpTx,
    status: Status,
    response: Response,
    errorMessage: Option[String] = None): Unit = {

    if (!tx.silent) {
      val fullRequestName = if (tx.redirectCount > 0)
        s"${tx.request.requestName} Redirect ${tx.redirectCount}"
      else tx.request.requestName

        def dump = {
          val buff = stringBuilder
          buff.append(Eol).append(">>>>>>>>>>>>>>>>>>>>>>>>>>").append(Eol)
          buff.append("Request:").append(Eol).append(s"$fullRequestName: $status ${errorMessage.getOrElse("")}").append(Eol)
          buff.append("=========================").append(Eol)
          buff.append("Session:").append(Eol).append(tx.session).append(Eol)
          buff.append("=========================").append(Eol)
          buff.append("HTTP request:").append(Eol).appendRequest(tx.request.ahcRequest, response.nettyRequest)
          buff.append("=========================").append(Eol)
          buff.append("HTTP response:").append(Eol).appendResponse(response).append(Eol)
          buff.append("<<<<<<<<<<<<<<<<<<<<<<<<<")
          buff.toString
        }

      if (status == KO) {
        logger.warn(s"Request '$fullRequestName' failed: ${errorMessage.getOrElse("")}")
        if (!logger.underlying.isTraceEnabled) logger.debug(dump)
      }
      logger.trace(dump)

      val extraInfo: List[Any] = try {
        tx.request.config.extraInfoExtractor match {
          case Some(extractor) => extractor(ExtraInfo(tx.request.requestName, status, tx.session, tx.request.ahcRequest, response))
          case _               => Nil
        }
      } catch {
        case e: Exception =>
          logger.warn("Encountered error while extracting extra request info", e)
          Nil
      }

      logRequestEnd(
        tx.session,
        fullRequestName,
        response.timings,
        status,
        errorMessage,
        extraInfo)
    }
  }

  /**
   * This method is used to send a message to the data writer actor and then execute the next action
   *
   * @param tx the HTTP transaction
   * @param update the update to be applied on the Session
   * @param status the status of the request
   * @param response the response
   */
  private def executeNext(tx: HttpTx, update: Session => Session, status: Status, response: Response): Unit = {

    val protocol = tx.request.config.protocol

    if (tx.primary)
      ResourceFetcher.resourceFetcherForFetchedPage(tx.request.ahcRequest, response, tx) match {
        case Some(resourceFetcher) =>
          actor(context, actorName("resourceFetcher"))(resourceFetcher())

        case None =>
          tx.next ! tx.session.increaseDrift(nowMillis - response.timings.responseEndDate)
      }

    else {
      val uri = response.request.getUri

      if (isCss(response.headers))
        tx.next ! CssResourceFetched(uri, status, update, tx.silent, response.statusCode, response.lastModifiedOrEtag(protocol), response.body.string)
      else
        tx.next ! RegularResourceFetched(uri, status, update, tx.silent)
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
    executeNext(newTx, update, status, response)
  }

  private def ko(tx: HttpTx, update: Session => Session, response: Response, message: String): Unit =
    logAndExecuteNext(tx, update, KO, response, Some(message))

  private def logGroupRequestUpdate(tx: HttpTx, status: Status, responseTimeInMillis: Long): Session => Session =
    if (tx.primary && !tx.silent)
      // resource logging is done in ResourceFetcher
      _.logGroupRequest(responseTimeInMillis, status)
    else
      Session.Identity

  /**
   * This method processes the response if needed for each checks given by the user
   */
  private def processResponse(tx: HttpTx, response: Response): Unit = {

      def redirectRequest(statusCode: Int, redirectUri: Uri, sessionWithUpdatedCookies: Session): Request = {
        val originalRequest = tx.request.ahcRequest

        val switchToGet = originalRequest.getMethod != "GET" && (statusCode == 303 || (statusCode == 302 && !tx.request.config.protocol.responsePart.strict302Handling))

        val newMethod = if (switchToGet) "GET" else originalRequest.getMethod

        val requestBuilder = new RequestBuilder(newMethod)
          .setUri(redirectUri)
          .setBodyEncoding(configuration.core.encoding)
          .setConnectionPoolKeyStrategy(originalRequest.getConnectionPoolPartitioning)
          .setInetAddress(originalRequest.getInetAddress)
          .setLocalInetAddress(originalRequest.getLocalAddress)
          .setVirtualHost(originalRequest.getVirtualHost)
          .setProxyServer(originalRequest.getProxyServer)
          .setRealm(originalRequest.getRealm)

        originalRequest.getHeaders.remove(HeaderNames.Host)
        originalRequest.getHeaders.remove(HeaderNames.ContentLength)
        originalRequest.getHeaders.remove(HeaderNames.Cookie)
        if (switchToGet) originalRequest.getHeaders.remove(HeaderNames.ContentType)

        for (cookie <- CookieHandling.getStoredCookies(sessionWithUpdatedCookies, redirectUri))
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
                  if (tx.request.config.protocol.requestPart.cache)
                    cacheRedirect(tx.request.ahcRequest, redirectURI)
                  else
                    Session.Identity

                val newUpdate = update andThen cacheRedirectUpdate
                val newSession = newUpdate(tx.session)

                val loggedTx = tx.copy(session = newSession, update = newUpdate)
                logRequest(loggedTx, OK, response)

                val newAhcRequest = redirectRequest(statusCode, redirectURI, newSession)
                val redirectTx = loggedTx.copy(request = loggedTx.request.copy(ahcRequest = newAhcRequest), redirectCount = tx.redirectCount + 1)
                HttpRequestAction.startHttpTransaction(redirectTx)

              case None =>
                ko(tx, update, response, "Redirect status, yet no Location header")

            }
        }

      def cacheRedirect(originalRequest: Request, redirectUri: Uri): Session => Session =
        response.statusCode match {
          case Some(code) if HttpHelper.isPermanentRedirect(code) =>
            val originalUri = originalRequest.getUri
            PermanentRedirect.addRedirect(_, originalUri, redirectUri)
          case _ => Session.Identity
        }

      def checkAndProceed(sessionUpdate: Session => Session, checks: List[HttpCheck]): Unit = {

        val (checkSaveUpdate, checkError) = Check.check(response, tx.session, checks)

        val status = checkError match {
          case None => OK
          case _    => KO
        }

        val cacheUpdate = CacheHandling.cache(tx.request.config.protocol, tx.request.ahcRequest, response)

        val totalUpdate = sessionUpdate andThen cacheUpdate andThen checkSaveUpdate

        logAndExecuteNext(tx, totalUpdate, status, response, checkError.map(_.message))
      }

    response.status match {

      case Some(status) =>
        val uri = tx.request.ahcRequest.getUri
        val storeCookiesUpdate: Session => Session =
          response.cookies match {
            case Nil     => Session.Identity
            case cookies => CookieHandling.storeCookies(_, uri, cookies)
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

          val storeRefererUpdate = RefererHandling.storeReferer(tx.request.ahcRequest, response, tx.request.config.protocol)

          checkAndProceed(newUpdate andThen storeRefererUpdate, checks)
        }

      case None =>
        ko(tx, Session.Identity, response, "How come OnComplete was sent with no status?!")
    }
  }
}

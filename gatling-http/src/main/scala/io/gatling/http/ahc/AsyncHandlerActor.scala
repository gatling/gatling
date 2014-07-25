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

import java.lang.{ StringBuilder => JStringBuilder }

import com.ning.http.client.uri.UriComponents
import com.ning.http.client.{ Request, RequestBuilder }
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
import io.gatling.http.check.{ HttpCheck, HttpCheckTarget }
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.fetch.{ CssResourceFetched, RegularResourceFetched, ResourceFetcher }
import io.gatling.http.referer.RefererHandling
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper
import io.gatling.http.util.HttpHelper.{ isCss, resolveFromURI }
import io.gatling.http.util.HttpStringBuilder

object AsyncHandlerActor extends AkkaDefaults {

  private var _instance: Option[ActorRef] = None

  def start(): Unit =
    if (!_instance.isDefined) {
      _instance = Some(system.actorOf(RoundRobinPool(3 * Runtime.getRuntime.availableProcessors).props(Props[AsyncHandlerActor])))
      system.registerOnTermination(_instance = None)
    }

  def instance() = _instance match {
    case Some(a) => a
    case _       => throw new UnsupportedOperationException("AsyncHandlerActor pool hasn't been started")
  }

  val propagatedOnRedirectHeaders = Vector(
    HeaderNames.Accept,
    HeaderNames.AcceptEncoding,
    HeaderNames.AcceptLanguage,
    HeaderNames.Referer,
    HeaderNames.UserAgent)
}

class AsyncHandlerActor extends BaseActor with DataWriterClient {

  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    message match {
      case Some(OnCompleted(tx, _)) =>
        logger.error(s"AsyncHandlerActor crashed on message $message, forwarding user to the next action", reason)
        tx.next ! tx.session.markAsFailed

      case Some(OnThrowable(tx, _, _)) =>
        logger.error(s"AsyncHandlerActor crashed on message $message, forwarding user to the next action", reason)
        tx.next ! tx.session.markAsFailed

      case _ =>
        logger.error(s"AsyncHandlerActor crashed on unknown message $message, dropping")
    }

  def receive = {
    case OnCompleted(tx, response)               => processResponse(tx, response)
    case OnThrowable(tx, response, errorMessage) => ko(tx, identity, response, errorMessage)
  }

  private def logRequest(
    tx: HttpTx,
    status: Status,
    response: Response,
    errorMessage: Option[String] = None): Unit = {

    if (!tx.request.config.silent) {
      val fullRequestName = if (tx.redirectCount > 0)
        s"${tx.request.requestName} Redirect ${tx.redirectCount}"
      else tx.request.requestName

        def dump = {
          val buff = new JStringBuilder
          buff.append(Eol).append(">>>>>>>>>>>>>>>>>>>>>>>>>>").append(Eol)
          buff.append("Request:").append(Eol).append(s"$fullRequestName: $status ${errorMessage.getOrElse("")}").append(Eol)
          buff.append("=========================").append(Eol)
          buff.append("Session:").append(Eol).append(tx.session).append(Eol)
          buff.append("=========================").append(Eol)
          buff.append("HTTP request:").append(Eol).appendAHCRequest(tx.request.ahcRequest)
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
          case Some(extractor) => extractor(tx.request.requestName, status, tx.session, tx.request.ahcRequest, response)
          case _               => Nil
        }
      } catch {
        case e: Exception =>
          logger.warn("Encountered error while extracting extra request info", e)
          Nil
      }

      writeRequestData(
        tx.session,
        fullRequestName,
        response.firstByteSent,
        response.lastByteSent,
        response.firstByteReceived,
        response.lastByteReceived,
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

    if (tx.primary) {
      val newSession = update(tx.session)

      ResourceFetcher.resourceFetcherForFetchedPage(tx.request.ahcRequest, response, tx, newSession) match {
        case Some(resourceFetcher) =>
          actor(context)(resourceFetcher())

        case None =>
          tx.next ! newSession.increaseDrift(nowMillis - response.lastByteReceived).logGroupRequest(response.reponseTimeInMillis, status)
      }

    } else {
      val uri = response.request.getURI

      if (isCss(response.headers))
        tx.next ! CssResourceFetched(uri, status, update, response.statusCode, response.lastModifiedOrEtag(protocol), response.body.string)
      else
        tx.next ! RegularResourceFetched(uri, status, update)
    }
  }

  private def logAndExecuteNext(tx: HttpTx, update: Session => Session, status: Status, response: Response, message: Option[String]): Unit = {

    val newTx = tx.copy(session = update(tx.session))

    logRequest(newTx, status, response, message)
    executeNext(newTx, update, status, response)
  }

  private def ko(tx: HttpTx, update: Session => Session, response: Response, message: String): Unit =
    logAndExecuteNext(tx, update andThen Session.MarkAsFailedUpdate, KO, response, Some(message))

  /**
   * This method processes the response if needed for each checks given by the user
   */
  private def processResponse(tx: HttpTx, response: Response): Unit = {

      def redirectRequest(redirectURI: UriComponents, sessionWithUpdatedCookies: Session): Request = {
        val originalRequest = tx.request.ahcRequest

        val requestBuilder = new RequestBuilder("GET")
          .setURI(redirectURI)
          .setBodyEncoding(configuration.core.encoding)
          .setConnectionPoolKeyStrategy(originalRequest.getConnectionPoolKeyStrategy)
          .setInetAddress(originalRequest.getInetAddress)
          .setLocalInetAddress(originalRequest.getLocalAddress)
          .setVirtualHost(originalRequest.getVirtualHost)
          .setProxyServer(originalRequest.getProxyServer)

        for {
          headerName <- AsyncHandlerActor.propagatedOnRedirectHeaders
          headerValue <- Option(originalRequest.getHeaders.getFirstValue(headerName))
        } requestBuilder.addHeader(headerName, headerValue)

        for (cookie <- CookieHandling.getStoredCookies(sessionWithUpdatedCookies, redirectURI))
          requestBuilder.addCookie(cookie)

        requestBuilder.build
      }

      def redirect(update: Session => Session): Unit =
        tx.request.config.maxRedirects match {
          case Some(maxRedirects) if maxRedirects == tx.redirectCount =>
            ko(tx, update, response, s"Too many redirects, max is $maxRedirects")

          case _ =>
            response.header(HeaderNames.Location) match {
              case Some(location) =>
                val redirectURI = resolveFromURI(tx.request.ahcRequest.getURI, location)

                val cacheRedirectUpdate = cacheRedirect(tx.request.ahcRequest, redirectURI)
                val logGroupRequestUpdate: Session => Session = {
                  val responseTime = response.reponseTimeInMillis
                  _.logGroupRequest(responseTime, OK)
                }
                val newUpdate = update andThen cacheRedirectUpdate andThen logGroupRequestUpdate
                val newSession = newUpdate(tx.session)

                val loggedTx = tx.copy(session = newSession, redirectCount = tx.redirectCount + 1, update = newUpdate)
                logRequest(loggedTx, OK, response)

                val newRequest = redirectRequest(redirectURI, newSession)
                val redirectTx = loggedTx.copy(request = loggedTx.request.copy(ahcRequest = newRequest))
                HttpRequestAction.startHttpTransaction(redirectTx)

              case None =>
                ko(tx, update, response, "Redirect status, yet no Location header")

            }
        }

      def cacheRedirect(originalRequest: Request, redirectURI: UriComponents): Session => Session =
        response.statusCode match {
          case Some(code) if HttpHelper.isPermanentRedirect(code) =>
            val originalURI = originalRequest.getURI
            PermanentRedirect.addRedirect(_, originalURI, redirectURI)
          case _ => Session.Identity
        }

      def checkAndProceed(sessionUpdate: Session => Session, checks: List[HttpCheck]): Unit = {

        val cacheUpdate = CacheHandling.cache(tx.request.config.protocol, tx.request.ahcRequest, response)

        val (checkSaveUpdate, checkError) = Check.check(response, tx.session, checks)
        val newUpdate = sessionUpdate andThen cacheUpdate andThen checkSaveUpdate

        checkError match {
          case None => logAndExecuteNext(tx, newUpdate, OK, response, None)
          case _    => logAndExecuteNext(tx, newUpdate, KO, response, checkError)
        }
      }

    response.status match {

      case Some(status) =>
        val uri = tx.request.ahcRequest.getURI
        val cookies = response.cookies
        val storeCookiesUpdate: Session => Session = CookieHandling.storeCookies(_, uri, cookies)
        val newUpdate = tx.update andThen storeCookiesUpdate

        if (HttpHelper.isRedirect(status.getStatusCode) && tx.request.config.followRedirect)
          redirect(newUpdate)

        else {
          val checks =
            if (HttpHelper.isNotModified(status.getStatusCode))
              tx.request.config.checks.filter(c => c.target != HttpCheckTarget.Body && c.target != HttpCheckTarget.Checksum)
            else
              tx.request.config.checks

          val storeRefererUpdate = RefererHandling.storeReferer(tx.request.ahcRequest, response, tx.request.config.protocol)

          checkAndProceed(newUpdate andThen storeRefererUpdate, checks)
        }

      case None =>
        ko(tx, identity, response, "How come OnComplete was sent with no status?!")
    }
  }
}

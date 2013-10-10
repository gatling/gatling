/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.collection.JavaConversions.asScalaBuffer

import com.ning.http.client.{ FluentStringsMap, RequestBuilder }
import com.ning.http.util.AsyncHttpProviderUtils

import akka.actor.ActorDSL.actor
import akka.actor.Props
import akka.routing.RoundRobinRouter
import io.gatling.core.akka.{ AkkaDefaults, BaseActor }
import io.gatling.core.check.Checks
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.writer.{ DataWriter, RequestMessage }
import io.gatling.core.session.Session
import io.gatling.core.util.StringHelper.eol
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Failure, Success }
import io.gatling.http.HeaderNames
import io.gatling.http.action.HttpRequestAction
import io.gatling.http.cache.CacheHandling
import io.gatling.http.check.{ HttpCheck, HttpCheckOrder }
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.dom.{ CssResourceFetched, HtmlResourceFetched, RegularResourceFetched, ResourceFetcher }
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper.{ isCss, isHtml }
import io.gatling.http.util.HttpStringBuilder

object AsyncHandlerActor extends AkkaDefaults {
	val redirectedRequestNamePattern = """(.+?) Redirect (\d+)""".r

	val asyncHandlerActor = system.actorOf(Props[AsyncHandlerActor].withRouter(RoundRobinRouter(nrOfInstances = 3 * Runtime.getRuntime.availableProcessors)))
}

class AsyncHandlerActor extends BaseActor {

	override def preRestart(reason: Throwable, message: Option[Any]) {
		logger.error(s"AsyncHandlerActor crashed on message $message, forwarding user to the next action", reason)
		message.foreach {
			case OnCompleted(tx, _) => tx.next ! tx.session.markAsFailed
			case OnThrowable(tx, _, _) => tx.next ! tx.session.markAsFailed
		}
	}

	def receive = {
		case OnCompleted(tx, response) => processResponse(tx, response)
		case OnThrowable(tx, response, errorMessage) => ko(tx, tx.session, response, errorMessage)
	}

	private def logRequest(
		tx: HttpTx,
		session: Session,
		status: Status,
		response: Response,
		errorMessage: Option[String] = None) {

		def dump = {
			val buff = new StringBuilder
			buff.append(eol).append(">>>>>>>>>>>>>>>>>>>>>>>>>>").append(eol)
			buff.append("Request:").append(eol).append(s"${tx.requestName}: $status ${errorMessage.getOrElse("")}").append(eol)
			buff.append("=========================").append(eol)
			buff.append("Session:").append(eol).append(session).append(eol)
			buff.append("=========================").append(eol)
			buff.append("HTTP request:").append(eol).appendAHCRequest(tx.request)
			buff.append("=========================").append(eol)
			buff.append("HTTP response:").append(eol).appendResponse(response).append(eol)
			buff.append("<<<<<<<<<<<<<<<<<<<<<<<<<")
			buff.toString
		}

		if (status == KO) {
			logger.warn(s"Request '${tx.requestName}' failed : ${errorMessage.getOrElse("")}")
			if (!logger.underlying.isTraceEnabled) logger.debug(dump)
		}
		logger.trace(dump)

		val extraInfo: List[Any] = try {
			tx.protocol.extraInfoExtractor.map(_(status, session, tx.request, response)).getOrElse(Nil)
		} catch {
			case e: Exception =>
				logger.warn("Encountered error while extracting extra request info", e)
				Nil
		}

		DataWriter.tell(RequestMessage(session.scenarioName, session.userId, session.groupStack, tx.requestName,
			response.firstByteSent, response.firstByteSent, response.firstByteReceived, response.lastByteReceived,
			status, errorMessage, extraInfo))
	}

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param newSession the new Session
	 */
	private def executeNext(tx: HttpTx, newSession: Session, status: Status, response: Response) {

		val nextSession = newSession.increaseDrift(nowMillis - response.lastByteReceived).logGroupRequest(response.reponseTimeInMillis, status)

		def statusCode() =
			if (response.hasResponseStatus)
				Some(response.getStatusCode)
			else None

		def body() =
			if (response.hasResponseStatus && response.getStatusCode == 200)
				Option(response.getResponseBody(configuration.core.encoding))
			else None

		if (tx.resourceFetching) {
			val resourceMessage =
				if (isCss(response.getHeaders)) {
					CssResourceFetched(response.request.getURI, status, body())

				} else if (isHtml(response.getHeaders)) {
					HtmlResourceFetched(response.request.getURI, status, statusCode(), body())

				} else
					RegularResourceFetched(response.request.getURI, status)

			tx.next ! resourceMessage

		} else if (tx.protocol.fetchHtmlResources && response.hasResponseStatus && isHtml(response.getHeaders)) {

			val resourceFetcher = ResourceFetcher(response.request.getURI, response.getStatusCode, Option(response.getHeader(HeaderNames.LAST_MODIFIED)), body())
			actor(context)(resourceFetcher(tx))

		} else
			tx.next ! nextSession
	}

	private def logAndExecuteNext(tx: HttpTx, session: Session, status: Status, response: Response, message: Option[String]) {
		logRequest(tx, session, status, response, message)
		executeNext(tx, session, status, response)
	}

	private def ok(tx: HttpTx, session: Session, response: Response) {
		logAndExecuteNext(tx, session, OK, response, None)
	}

	private def ko(tx: HttpTx, session: Session, response: Response, message: String) {
		logAndExecuteNext(tx, session.markAsFailed, KO, response, Some(message))
	}

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(tx: HttpTx, response: Response) {

		def redirect(sessionWithUpdatedCookies: Session) {

			if (tx.protocol.maxRedirects.map(_ == tx.numberOfRedirects).getOrElse(false)) {
				ko(tx, sessionWithUpdatedCookies, response, s"Too many redirects, max is ${tx.protocol.maxRedirects.get}")

			} else {
				logRequest(tx, sessionWithUpdatedCookies, OK, response)

				val redirectURI = AsyncHttpProviderUtils.getRedirectUri(tx.request.getURI, response.getHeader(HeaderNames.LOCATION))

				val requestBuilder = new RequestBuilder(tx.request)
					.setMethod("GET")
					.setBodyEncoding(configuration.core.encoding)
					.setQueryParameters(null.asInstanceOf[FluentStringsMap])
					.setParameters(null.asInstanceOf[FluentStringsMap])
					.setUrl(redirectURI.toString)
					.setConnectionPoolKeyStrategy(tx.request.getConnectionPoolKeyStrategy)

				for (cookie <- CookieHandling.getStoredCookies(sessionWithUpdatedCookies, redirectURI))
					requestBuilder.addOrReplaceCookie(cookie)

				val newRequest = requestBuilder.build
				newRequest.getHeaders.remove(HeaderNames.CONTENT_LENGTH)
				newRequest.getHeaders.remove(HeaderNames.CONTENT_TYPE)

				val newRequestName = tx.requestName match {
					case AsyncHandlerActor.redirectedRequestNamePattern(requestBaseName, redirectCount) => s"$requestBaseName Redirect ${redirectCount.toInt + 1}"
					case _ => tx.requestName + " Redirect 1"
				}

				val redirectTx = tx.copy(session = sessionWithUpdatedCookies, request = newRequest, requestName = newRequestName, numberOfRedirects = tx.numberOfRedirects + 1)
				HttpRequestAction.handleHttpTransaction(redirectTx)
			}
		}

		def checkAndProceed(sessionWithUpdatedCookies: Session, checks: List[HttpCheck]) {
			val sessionWithUpdatedCache = CacheHandling.cache(tx.protocol, sessionWithUpdatedCookies, tx.request, response)
			val checkResult = Checks.check(response, sessionWithUpdatedCache, checks)

			checkResult match {
				case Success(newSession) => ok(tx, newSession, response)
				case Failure(errorMessage) => ko(tx, sessionWithUpdatedCache, response, errorMessage)
			}
		}

		val sessionWithUpdatedCookies = CookieHandling.storeCookies(tx.session, response.getUri, response.getCookies.toList)

		if (response.getStatusCode == 304)
			checkAndProceed(sessionWithUpdatedCookies, tx.checks.filter(c => c.order != HttpCheckOrder.Body && c.order != HttpCheckOrder.Checksum))
		else if (response.isRedirected && tx.protocol.followRedirect)
			redirect(sessionWithUpdatedCookies)
		else
			checkAndProceed(sessionWithUpdatedCookies, tx.checks)
	}
}

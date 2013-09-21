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
import scala.concurrent.duration.DurationInt

import com.ning.http.client.{ FluentStringsMap, RequestBuilder }
import com.ning.http.util.AsyncHttpProviderUtils

import akka.actor.Props
import akka.routing.RoundRobinRouter
import io.gatling.core.action.{ BaseActor, system }
import io.gatling.core.check.Checks
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.writer.{ DataWriter, RequestMessage }
import io.gatling.core.session.Session
import io.gatling.core.util.StringHelper.eol
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Failure, Success }
import io.gatling.http.Headers.{ Names => HeaderNames }
import io.gatling.http.cache.CacheHandling
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.response.Response
import io.gatling.http.util.HttpStringBuilder

object AsyncHandlerActor {
	val redirectedRequestNamePattern = """(.+?) Redirect (\d+)""".r
	val timeout = configuration.core.timeOut.simulation seconds

	val asyncHandlerActor = system.actorOf(Props[AsyncHandlerActor].withRouter(RoundRobinRouter(nrOfInstances = 3 * Runtime.getRuntime.availableProcessors)))
}

class AsyncHandlerActor extends BaseActor {

	override def preStart {
		context.setReceiveTimeout(AsyncHandlerActor.timeout)
	}

	override def preRestart(reason: Throwable, message: Option[Any]) {
		logger.error(s"AsyncHandlerActor crashed on message $message, forwarding user to the next action", reason)
		message.foreach {
			case OnCompleted(task, _) => task.next ! task.session.markAsFailed
			case OnThrowable(task, _, _) => task.next ! task.session.markAsFailed
		}
	}

	def receive = {
		case OnCompleted(task, response) => processResponse(task, response)
		case OnThrowable(task, response, errorMessage) => ko(task, task.session, response, errorMessage)
	}

	private def logRequest(
		task: HttpTask,
		session: Session,
		status: Status,
		response: Response,
		errorMessage: Option[String] = None) {

		def dump = {
			val buff = new StringBuilder
			buff.append(eol).append(">>>>>>>>>>>>>>>>>>>>>>>>>>").append(eol)
			buff.append("Request:").append(eol).append(s"${task.requestName}: $status ${errorMessage.getOrElse("")}").append(eol)
			buff.append("=========================").append(eol)
			buff.append("Session:").append(eol).append(session).append(eol)
			buff.append("=========================").append(eol)
			buff.append("HTTP request:").append(eol).appendAHCRequest(task.request)
			buff.append("=========================").append(eol)
			buff.append("HTTP response:").append(eol).appendResponse(response).append(eol)
			buff.append("<<<<<<<<<<<<<<<<<<<<<<<<<")
			buff.toString
		}

		if (status == KO) {
			logger.warn(s"Request '${task.requestName}' failed : ${errorMessage.getOrElse("")}")
			if (!logger.underlying.isTraceEnabled) logger.debug(dump)
		}
		logger.trace(dump)

		val extraInfo: List[Any] = try {
			task.protocol.extraInfoExtractor.map(_(status, session, task.request, response)).getOrElse(Nil)
		} catch {
			case e: Exception =>
				logger.warn("Encountered error while extracting extra request info", e)
				Nil
		}

		DataWriter.tell(RequestMessage(session.scenarioName, session.userId, session.groupStack, task.requestName,
			response.firstByteSent, response.firstByteSent, response.firstByteReceived, response.lastByteReceived,
			status, errorMessage, extraInfo))
	}

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param newSession the new Session
	 */
	private def executeNext(task: HttpTask, newSession: Session, status: Status, response: Response) {
		task.next ! newSession.increaseDrift(nowMillis - response.lastByteReceived).logGroupRequest(response.reponseTimeInMillis, status)
	}

	private def logAndExecuteNext(task: HttpTask, session: Session, status: Status, response: Response, message: Option[String]) {
		logRequest(task, session, status, response, message)
		executeNext(task, session, status, response)
	}

	private def ok(task: HttpTask, session: Session, response: Response) {
		logAndExecuteNext(task, session, OK, response, None)
	}

	private def ko(task: HttpTask, session: Session, response: Response, message: String) {
		logAndExecuteNext(task, session.markAsFailed, KO, response, Some(message))
	}

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(task: HttpTask, response: Response) {

		def redirect(sessionWithUpdatedCookies: Session) {

			logRequest(task, sessionWithUpdatedCookies, OK, response)

			val redirectURI = AsyncHttpProviderUtils.getRedirectUri(task.request.getURI, response.getHeader(HeaderNames.LOCATION))

			val requestBuilder = new RequestBuilder(task.request)
				.setMethod("GET")
				.setBodyEncoding(configuration.core.encoding)
				.setQueryParameters(null.asInstanceOf[FluentStringsMap])
				.setParameters(null.asInstanceOf[FluentStringsMap])
				.setUrl(redirectURI.toString)
				.setConnectionPoolKeyStrategy(task.request.getConnectionPoolKeyStrategy)

			for (cookie <- CookieHandling.getStoredCookies(sessionWithUpdatedCookies, redirectURI))
				requestBuilder.addOrReplaceCookie(cookie)

			val newRequest = requestBuilder.build
			newRequest.getHeaders.remove(HeaderNames.CONTENT_LENGTH)
			newRequest.getHeaders.remove(HeaderNames.CONTENT_TYPE)

			val newRequestName = task.requestName match {
				case AsyncHandlerActor.redirectedRequestNamePattern(requestBaseName, redirectCount) => requestBaseName + " Redirect " + (redirectCount.toInt + 1)
				case _ => task.requestName + " Redirect 1"
			}

			HttpClient.sendHttpRequest(task.copy(session = sessionWithUpdatedCookies, request = newRequest, requestName = newRequestName))
		}

		def checkAndProceed(sessionWithUpdatedCookies: Session) {
			val sessionWithUpdatedCache = CacheHandling.cache(task.protocol, sessionWithUpdatedCookies, task.request, response)
			val checkResult = Checks.check(response, sessionWithUpdatedCache, task.checks)

			checkResult match {
				case Success(newSession) => ok(task, newSession, response)
				case Failure(errorMessage) => ko(task, sessionWithUpdatedCache, response, errorMessage)
			}
		}

		val sessionWithUpdatedCookies = CookieHandling.storeCookies(task.session, response.getUri, response.getCookies.toList)

		if (response.isRedirected && task.protocol.followRedirect)
			redirect(sessionWithUpdatedCookies)
		else
			checkAndProceed(sessionWithUpdatedCookies)
	}
}
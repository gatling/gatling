/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.http.ahc

import java.lang.System.currentTimeMillis
import java.net.URLDecoder

import scala.annotation.tailrec
import scala.collection.JavaConverters.asScalaBufferConverter

import com.excilys.ebi.gatling.core.check.Check.applyChecks
import com.excilys.ebi.gatling.core.check.Failure
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ RequestStatus, OK, KO }
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.Headers.{ Names => HeaderNames }
import com.excilys.ebi.gatling.http.action.HttpRequestAction.HTTP_CLIENT
import com.excilys.ebi.gatling.http.ahc.GatlingAsyncHandlerActor.REDIRECTED_REQUEST_NAME_PATTERN
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.cookie.CookieHandling
import com.excilys.ebi.gatling.http.request.HttpPhase.HttpPhase
import com.excilys.ebi.gatling.http.request.HttpPhase
import com.excilys.ebi.gatling.http.util.HttpHelper.{ toRichResponse, computeRedirectUrl }
import com.ning.http.client.Response.ResponseBuilder
import com.ning.http.client.{ Response, RequestBuilder, Request, FluentStringsMap }

import akka.actor.{ ActorRef, Actor }
import grizzled.slf4j.Logging

object GatlingAsyncHandlerActor {
	val REDIRECTED_REQUEST_NAME_PATTERN = """(.+?) Redirect (\d+)""".r
}

class GatlingAsyncHandlerActor(session: Session, checks: List[HttpCheck], next: ActorRef, requestName: String, originalRequest: Request, followRedirect: Boolean) extends Actor with Logging with CookieHandling {

	val responseBuilder = new ResponseBuilder
	val requestStartDate = currentTimeMillis
	var endOfRequestSendingDate = 0L
	var startOfResponseReceivingDate = 0L
	var responseEndDate = 0L

	def receive = {
		case OnHeaderWriteCompleted(time) => endOfRequestSendingDate = time

		case OnContentWriteCompleted(time) => endOfRequestSendingDate = time

		case OnStatusReceived(responseStatus, time) =>
			startOfResponseReceivingDate = time
			responseBuilder.accumulate(responseStatus)

		case OnHeadersReceived(headers) => responseBuilder.accumulate(headers)

		case OnBodyPartReceived(bodyPart) => responseBuilder.accumulate(bodyPart)

		case OnCompleted(time) =>
			responseEndDate = time
			processResponse(responseBuilder.build)
			context.stop(self)

		case OnThrowable(errorMessage, time) =>
			endOfRequestSendingDate = if (endOfRequestSendingDate != 0L) endOfRequestSendingDate else time
			startOfResponseReceivingDate = if (startOfResponseReceivingDate != 0L) startOfResponseReceivingDate else time
			responseEndDate = if (responseEndDate != 0L) responseEndDate else time
			logAndExecuteNext(session, KO, Some(errorMessage))
			context.stop(self)

		case m =>
			throw new IllegalArgumentException("Unknown message type " + m)
	}

	private def logRequest(requestResult: RequestStatus, requestMessage: Option[String] = None) = DataWriter.logRequest(session.scenarioName, session.userId, "Request " + requestName, requestStartDate, responseEndDate, endOfRequestSendingDate, endOfRequestSendingDate, requestResult, requestMessage.getOrElse("Request executed successfully"))

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param session the new Session
	 * @param requestResult the result of the request
	 * @param requestMessage the message that will be logged
	 * @param processingStartDate date of the beginning of the response processing
	 */
	private def logAndExecuteNext(newSession: Session, requestResult: RequestStatus, requestMessage: Option[String] = None) {
		logRequest(requestResult, requestMessage)
		next ! newSession.setAttribute(Session.LAST_ACTION_DURATION_KEY, currentTimeMillis - responseEndDate)
	}

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(response: Response) {

		def handleFollowRedirect(sessionWithUpdatedCookies: Session) {

			val redirectUrl = computeRedirectUrl(URLDecoder.decode(response.getHeader(HeaderNames.LOCATION), configuration.encoding), originalRequest.getUrl)

			val requestBuilder = new RequestBuilder(originalRequest).setMethod("GET").setQueryParameters(null.asInstanceOf[FluentStringsMap]).setParameters(null.asInstanceOf[FluentStringsMap]).setUrl(redirectUrl)

			for (cookie <- getStoredCookies(sessionWithUpdatedCookies, redirectUrl))
				requestBuilder.addCookie(cookie)

			val request = requestBuilder.build
			request.getHeaders.remove(HeaderNames.CONTENT_LENGTH)

			val newRequestName = REDIRECTED_REQUEST_NAME_PATTERN.findFirstMatchIn(requestName) match {
				case Some(nameMatch) =>
					val requestBaseName = nameMatch.group(1)
					val redirectCount = nameMatch.group(2).toInt
					new StringBuilder().append(requestBaseName).append(" Redirect ").append(redirectCount + 1).toString
				case None => requestName + " Redirect 1"
			}

			logRequest(OK)
			HTTP_CLIENT.executeRequest(request, new GatlingAsyncHandler(sessionWithUpdatedCookies, checks, next, newRequestName, request, followRedirect))
		}

		@tailrec
		def checkPhasesRec(session: Session, phases: List[HttpPhase]) {

			phases match {
				case Nil => logAndExecuteNext(session, OK)
				case phase :: otherPhases =>
					val phaseChecks = checks.filter(_.phase == phase)
					var (newSessionWithSavedValues, checkResult) = applyChecks(session, response, phaseChecks)

					checkResult match {
						case Failure(errorMessage) =>
							if (isDebugEnabled)
								debug(new StringBuilder().append("Check on request '").append(requestName).append("' failed : ").append(errorMessage).append(", response was:").append(response.dump))
							else
								warn(new StringBuilder().append("Check on request '").append(requestName).append("' failed : ").append(errorMessage))

							logAndExecuteNext(newSessionWithSavedValues, KO, Some(errorMessage))
						case _ => checkPhasesRec(newSessionWithSavedValues, otherPhases)
					}
			}
		}

		val sessionWithUpdatedCookies = storeCookies(session, response.getUri.toString, response.getCookies.asScala)

		if ((response.getStatusCode == 301 || response.getStatusCode == 302) && followRedirect) {
			handleFollowRedirect(sessionWithUpdatedCookies)

		} else
			checkPhasesRec(sessionWithUpdatedCookies, HttpPhase.phases)
	}
}
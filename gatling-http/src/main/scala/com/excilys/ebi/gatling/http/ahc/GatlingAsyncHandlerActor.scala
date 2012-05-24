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
import scala.collection.JavaConversions.asScalaBuffer

import com.excilys.ebi.gatling.core.check.Check.applyChecks
import com.excilys.ebi.gatling.core.check.Failure
import com.excilys.ebi.gatling.core.config.GatlingConfiguration
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ RequestStatus, OK, KO }
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.Headers.{ Names => HeaderNames }
import com.excilys.ebi.gatling.http.action.HttpRequestAction.HTTP_CLIENT
import com.excilys.ebi.gatling.http.ahc.GatlingAsyncHandlerActor.{ REDIRECT_STATUS_CODES, REDIRECTED_REQUEST_NAME_PATTERN }
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpConfig
import com.excilys.ebi.gatling.http.cookie.CookieHandling
import com.excilys.ebi.gatling.http.request.HttpPhase.HttpPhase
import com.excilys.ebi.gatling.http.request.HttpPhase
import com.excilys.ebi.gatling.http.util.HttpHelper.{ toRichResponse, computeRedirectUrl }
import com.ning.http.client.{ Response, RequestBuilder, Request, FluentStringsMap }

import akka.actor.{ ReceiveTimeout, ActorRef, Actor }
import akka.util.duration.intToDurationInt
import grizzled.slf4j.Logging

object GatlingAsyncHandlerActor {
	val REDIRECTED_REQUEST_NAME_PATTERN = """(.+?) Redirect (\d+)""".r
	val REDIRECT_STATUS_CODES = 301 to 303
}

class GatlingAsyncHandlerActor(var session: Session, checks: List[HttpCheck], next: ActorRef, var requestName: String, var request: Request, followRedirect: Boolean, gatlingConfiguration: GatlingConfiguration) extends Actor with Logging with CookieHandling {

	var executionStartDate = currentTimeMillis
	var requestSendingEndDate = 0L
	var responseReceivingStartDate = 0L
	var executionEndDate = 0L

	resetTimeout

	def receive = {
		case OnHeaderWriteCompleted(time) =>
			resetTimeout
			requestSendingEndDate = time

		case OnContentWriteCompleted(time) =>
			resetTimeout
			requestSendingEndDate = time

		case OnStatusReceived(time) =>
			resetTimeout
			responseReceivingStartDate = time

		case OnCompleted(response, time) =>
			executionEndDate = time
			processResponse(response)

		case OnThrowable(errorMessage, time) =>
			requestSendingEndDate = if (requestSendingEndDate != 0L) requestSendingEndDate else time
			responseReceivingStartDate = if (responseReceivingStartDate != 0L) responseReceivingStartDate else time
			executionEndDate = time
			logRequest(KO, errorMessage)
			executeNext(session)

		case ReceiveTimeout =>
			error("GatlingAsyncHandlerActor timed out")
			logRequest(KO, "GatlingAsyncHandlerActor timed out")
			executeNext(session)

		case m => throw new IllegalArgumentException("Unknown message type " + m)
	}

	def resetTimeout = context.setReceiveTimeout(HttpConfig.GATLING_HTTP_CONFIG_REQUEST_TIMEOUT milliseconds)

	private def logRequest(requestResult: RequestStatus, requestMessage: String = "Request executed successfully") = DataWriter.logRequest(session.scenarioName, session.userId, "Request " + requestName, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, requestResult, requestMessage)

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param session the new Session
	 */
	private def executeNext(newSession: Session) {
		next ! newSession.setAttribute(Session.LAST_ACTION_DURATION_KEY, currentTimeMillis - executionEndDate)
		context.stop(self)
	}

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(response: Response) {

		def handleFollowRedirect(sessionWithUpdatedCookies: Session) {

			def configureForNextRedirect(newSession: Session, newRequestName: String, newRequest: Request) {
				this.session = newSession
				this.requestName = newRequestName
				this.request = newRequest
				this.executionStartDate = currentTimeMillis
				this.requestSendingEndDate = 0L
				this.responseReceivingStartDate = 0L
				this.executionEndDate = 0L
			}

			logRequest(OK)

			val redirectUrl = computeRedirectUrl(URLDecoder.decode(response.getHeader(HeaderNames.LOCATION), gatlingConfiguration.encoding), request.getUrl)

			val requestBuilder = new RequestBuilder(request).setMethod("GET").setQueryParameters(null.asInstanceOf[FluentStringsMap]).setParameters(null.asInstanceOf[FluentStringsMap]).setUrl(redirectUrl)

			for (cookie <- getStoredCookies(sessionWithUpdatedCookies, redirectUrl))
				requestBuilder.addCookie(cookie)

			val newRequest = requestBuilder.build
			newRequest.getHeaders.remove(HeaderNames.CONTENT_LENGTH)

			val newRequestName = REDIRECTED_REQUEST_NAME_PATTERN.findFirstMatchIn(requestName) match {
				case Some(nameMatch) =>
					val requestBaseName = nameMatch.group(1)
					val redirectCount = nameMatch.group(2).toInt
					new StringBuilder().append(requestBaseName).append(" Redirect ").append(redirectCount + 1).toString
				case None => requestName + " Redirect 1"
			}

			configureForNextRedirect(sessionWithUpdatedCookies, newRequestName, newRequest)

			HTTP_CLIENT.executeRequest(newRequest, new GatlingAsyncHandler(checks, newRequestName, self))
		}

		@tailrec
		def checkPhasesRec(session: Session, phases: List[HttpPhase]) {

			phases match {
				case Nil =>
					logRequest(OK)
					executeNext(session)

				case phase :: otherPhases =>
					val phaseChecks = checks.filter(_.phase == phase)
					var (newSession, checkResult) = applyChecks(session, response, phaseChecks)

					checkResult match {
						case Failure(errorMessage) =>
							if (isDebugEnabled)
								debug(new StringBuilder().append("Check on request '").append(requestName).append("' failed : ").append(errorMessage).append(", response was:").append(response.dump))
							else
								warn(new StringBuilder().append("Check on request '").append(requestName).append("' failed : ").append(errorMessage))

							logRequest(KO, errorMessage)
							executeNext(newSession)

						case _ => checkPhasesRec(newSession, otherPhases)
					}
			}
		}

		val sessionWithUpdatedCookies = storeCookies(session, response.getUri, response.getCookies)

		if (REDIRECT_STATUS_CODES.contains(response.getStatusCode) && followRedirect)
			handleFollowRedirect(sessionWithUpdatedCookies)
		else
			checkPhasesRec(sessionWithUpdatedCookies, HttpPhase.phases)
	}
}
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

import java.lang.System.{ nanoTime, currentTimeMillis }
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
import com.excilys.ebi.gatling.http.config.{ HttpProtocolConfiguration, HttpConfig }
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

class GatlingAsyncHandlerActor(var session: Session, checks: List[HttpCheck[_]], next: ActorRef,
	var requestName: String, var request: Request, followRedirect: Boolean,
	protocolConfiguration: Option[HttpProtocolConfiguration],
	gatlingConfiguration: GatlingConfiguration,
	handlerFactory: HandlerFactory, responseBuilderFactory: ExtendedResponseBuilderFactory)
		extends Actor with Logging {

	var responseBuilder = responseBuilderFactory(session)
	var executionStartDate = currentTimeMillis
	var executionStartDateNanos = nanoTime
	var requestSendingEndDate = 0L
	var responseReceivingStartDate = 0L
	var executionEndDate = 0L

	resetTimeout

	private def computeTimeFromNanos(nanos: Long) = (nanos - executionStartDateNanos) / 1000000 + executionStartDate

	def receive = {
		case OnHeaderWriteCompleted(nanos) =>
			resetTimeout
			requestSendingEndDate = computeTimeFromNanos(nanos)

		case OnContentWriteCompleted(nanos) =>
			resetTimeout
			requestSendingEndDate = computeTimeFromNanos(nanos)

		case OnStatusReceived(status, nanos) =>
			resetTimeout
			responseReceivingStartDate = computeTimeFromNanos(nanos)
			responseBuilder.accumulate(status)

		case OnHeadersReceived(headers) =>
			resetTimeout
			responseBuilder.accumulate(headers)

		case OnBodyPartReceived(bodyPart) =>
			resetTimeout
			responseBuilder.accumulate(bodyPart)

		case OnCompleted(nanos) =>
			executionEndDate = computeTimeFromNanos(nanos)
			processResponse(responseBuilder.build)

		case OnThrowable(errorMessage, nanos) =>
			executionEndDate = computeTimeFromNanos(nanos)
			requestSendingEndDate = if (requestSendingEndDate != 0L) requestSendingEndDate else executionEndDate
			responseReceivingStartDate = if (responseReceivingStartDate != 0L) responseReceivingStartDate else executionEndDate
			logRequest(KO, Some(errorMessage))
			executeNext(session)

		case ReceiveTimeout =>
			error("GatlingAsyncHandlerActor timed out")
			logRequest(KO, Some("GatlingAsyncHandlerActor timed out"))
			executeNext(session)

		case m => throw new IllegalArgumentException("Unknown message type " + m)
	}

	def resetTimeout = context.setReceiveTimeout(HttpConfig.GATLING_HTTP_CONFIG_REQUEST_TIMEOUT_IN_MS milliseconds)

	private def logRequest(requestResult: RequestStatus,
		requestMessage: Option[String] = None,
		response: Option[Response] = None) = {
		DataWriter.logRequest(session.scenarioName, session.userId, "Request " + requestName,
			executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate,
			requestResult, requestMessage, extractExtraInfo(response))
	}

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param newSession the new Session
	 */
	private def executeNext(newSession: Session) {
		next ! newSession.increaseTimeShift(currentTimeMillis - executionEndDate)
		context.stop(self)
	}

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(response: Response) {

		def handleFollowRedirect(sessionWithUpdatedCookies: Session) {

			def configureForNextRedirect(newSession: Session, newRequestName: String, newRequest: Request) {
				this.session = newSession
				this.responseBuilder = responseBuilderFactory(session)
				this.requestName = newRequestName
				this.request = newRequest
				this.executionStartDate = currentTimeMillis
				this.executionStartDateNanos = nanoTime
				this.requestSendingEndDate = 0L
				this.responseReceivingStartDate = 0L
				this.executionEndDate = 0L
			}

			logRequest(OK, response = Some(response))

			val redirectUrl = computeRedirectUrl(URLDecoder.decode(response.getHeader(HeaderNames.LOCATION), gatlingConfiguration.encoding), request.getUrl)

			val requestBuilder = new RequestBuilder(request).setMethod("GET").setQueryParameters(null.asInstanceOf[FluentStringsMap]).setParameters(null.asInstanceOf[FluentStringsMap]).setUrl(redirectUrl)

			for (cookie <- CookieHandling.getStoredCookies(sessionWithUpdatedCookies, redirectUrl))
				requestBuilder.addCookie(cookie)

			val newRequest = requestBuilder.build
			newRequest.getHeaders.remove(HeaderNames.CONTENT_LENGTH)

			val newRequestName = requestName match {
				case REDIRECTED_REQUEST_NAME_PATTERN(requestBaseName, redirectCount) =>
					new StringBuilder().append(requestBaseName).append(" Redirect ").append(redirectCount.toInt + 1).toString

				case _ =>
					requestName + " Redirect 1"
			}

			configureForNextRedirect(sessionWithUpdatedCookies, newRequestName, newRequest)

			HTTP_CLIENT.executeRequest(newRequest, handlerFactory(newRequestName, self))
		}

		@tailrec
		def checkPhasesRec(session: Session, phases: List[HttpPhase]) {

			phases match {
				case Nil =>
					logRequest(OK, response = Some(response))
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

							logRequest(KO, Some(errorMessage), Some(response))
							executeNext(newSession)

						case _ => checkPhasesRec(newSession, otherPhases)
					}
			}
		}

		val sessionWithUpdatedCookies = CookieHandling.storeCookies(session, response.getUri, response.getCookies.toList)

		if (REDIRECT_STATUS_CODES.contains(response.getStatusCode) && followRedirect)
			handleFollowRedirect(sessionWithUpdatedCookies)
		else
			checkPhasesRec(sessionWithUpdatedCookies, HttpPhase.phases)
	}

	/**
	 * Extract extra info from both request and response.
	 *
	 * @param response is the response to extract data from; request is retrieved from the property
	 * @return the extracted Strings
	 */
	private def extractExtraInfo(response: Option[Response] = None): List[String] = {

		def extractExtraRequestInfo(protocolConfiguration: Option[HttpProtocolConfiguration], request: Request): List[String] = {
			val extracted = try {
				for (
					httpProtocolConfig <- protocolConfiguration;
					extractor <- httpProtocolConfig.extraRequestInfoExtractor
				) yield extractor(request)

			} catch {
				case e: Exception =>
					warn("Encountered error while extracting extra request info", e)
					None
			}

			extracted.getOrElse(Nil)
		}

		def extractExtraResponseInfo(protocolConfiguration: Option[HttpProtocolConfiguration], response: Option[Response]): List[String] = {
			val extracted = try {
				for (
					httpProtocolConfig <- protocolConfiguration;
					extractor <- httpProtocolConfig.extraResponseInfoExtractor;
					response <- response
				) yield extractor(response)

			} catch {
				case e: Exception =>
					warn("Encountered error while extracting extra response info", e)
					None
			}

			extracted.getOrElse(Nil)
		}

		val extraRequestInfo = extractExtraRequestInfo(protocolConfiguration, request)
		val extraResponseInfo = extractExtraResponseInfo(protocolConfiguration, response)
		extraRequestInfo ::: extraResponseInfo
	}
}
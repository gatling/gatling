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
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ RequestStatus, OK, KO }
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.Headers.{ Names => HeaderNames }
import com.excilys.ebi.gatling.http.action.HttpRequestAction.HTTP_CLIENT
import com.excilys.ebi.gatling.http.ahc.GatlingAsyncHandlerActor.{ REDIRECT_STATUS_CODES, REDIRECTED_REQUEST_NAME_PATTERN }
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.{ HttpProtocolConfiguration, HttpConfig }
import com.excilys.ebi.gatling.http.cookie.CookieHandling
import com.excilys.ebi.gatling.http.request.HttpPhase
import com.excilys.ebi.gatling.http.request.HttpPhase.HttpPhase
import com.excilys.ebi.gatling.http.response.{ ExtendedResponseBuilderFactory, ExtendedResponseBuilder, ExtendedResponse }
import com.excilys.ebi.gatling.http.util.HttpHelper.computeRedirectUrl
import com.ning.http.client.{ RequestBuilder, Request, FluentStringsMap }

import akka.actor.{ ReceiveTimeout, ActorRef, Actor }
import akka.util.duration.intToDurationInt
import grizzled.slf4j.Logging

object GatlingAsyncHandlerActor {
	val REDIRECTED_REQUEST_NAME_PATTERN = """(.+?) Redirect (\d+)""".r
	val REDIRECT_STATUS_CODES = 301 to 303

	def newAsyncHandlerActorFactory(
		checks: List[HttpCheck[_]],
		next: ActorRef,
		requestName: String,
		protocolConfiguration: Option[HttpProtocolConfiguration],
		gatlingConfiguration: GatlingConfiguration) = {

		val followRedirect = protocolConfiguration.map(_.followRedirectEnabled).getOrElse(true)
		val handlerFactory = GatlingAsyncHandler.newHandlerFactory(checks)
		val responseBuilderFactory = ExtendedResponseBuilder.newExtendedResponseBuilder(checks)

		(request: Request, session: Session) =>
			new GatlingAsyncHandlerActor(
				session,
				checks,
				next,
				requestName,
				request,
				followRedirect,
				protocolConfiguration,
				gatlingConfiguration,
				handlerFactory,
				responseBuilderFactory)
	}
}

class GatlingAsyncHandlerActor(var session: Session, checks: List[HttpCheck[_]], next: ActorRef,
	var requestName: String, var request: Request, followRedirect: Boolean,
	protocolConfiguration: Option[HttpProtocolConfiguration],
	gatlingConfiguration: GatlingConfiguration,
	handlerFactory: HandlerFactory, responseBuilderFactory: ExtendedResponseBuilderFactory)
	extends Actor with Logging {

	var responseBuilder = responseBuilderFactory(request, session)

	resetTimeout

	def receive = {
		case OnHeaderWriteCompleted(nanos) =>
			resetTimeout
			responseBuilder.updateRequestSendingEndDate(nanos)

		case OnContentWriteCompleted(nanos) =>
			resetTimeout
			responseBuilder.updateRequestSendingEndDate(nanos)

		case OnStatusReceived(status, nanos) =>
			resetTimeout
			responseBuilder.updateResponseReceivingStartDate(nanos)
			responseBuilder.accumulate(status)

		case OnHeadersReceived(headers) =>
			resetTimeout
			responseBuilder.accumulate(headers)

		case OnBodyPartReceived(bodyPart) =>
			resetTimeout
			responseBuilder.accumulate(bodyPart)

		case OnCompleted(nanos) =>
			responseBuilder.computeExecutionEndDateFromNanos(nanos)
			processResponse(responseBuilder.build)

		case OnThrowable(errorMessage, nanos) =>
			responseBuilder.computeExecutionEndDateFromNanos(nanos)
			val response = responseBuilder.build
			logRequest(KO, response, Some(errorMessage))
			executeNext(session, response)

		case ReceiveTimeout =>
			error("GatlingAsyncHandlerActor timed out")
			val response = responseBuilder.build
			logRequest(KO, response, Some("GatlingAsyncHandlerActor timed out"))
			executeNext(session, response)

		case m => throw new IllegalArgumentException("Unknown message type " + m)
	}

	def resetTimeout = context.setReceiveTimeout(HttpConfig.GATLING_HTTP_CONFIG_REQUEST_TIMEOUT_IN_MS milliseconds)

	private def logRequest(requestResult: RequestStatus,
		response: ExtendedResponse,
		requestMessage: Option[String] = None) = {
		DataWriter.logRequest(session.scenarioName, session.userId, requestName,
			response.executionStartDate, response.executionEndDate, response.requestSendingEndDate, response.responseReceivingStartDate,
			requestResult, requestMessage, extractExtraInfo(response))
	}

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param newSession the new Session
	 */
	private def executeNext(newSession: Session, response: ExtendedResponse) {
		next ! newSession.increaseTimeShift(currentTimeMillis - response.executionEndDate)
		context.stop(self)
	}

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(response: ExtendedResponse) {

		def handleFollowRedirect(sessionWithUpdatedCookies: Session) {

			def configureForNextRedirect(newSession: Session, newRequestName: String, newRequest: Request) {
				this.session = newSession
				this.responseBuilder = responseBuilderFactory(request, session)
				this.requestName = newRequestName
				this.request = newRequest
			}

			logRequest(OK, response)

			val redirectUrl = computeRedirectUrl(response.getHeader(HeaderNames.LOCATION), request.getUrl)
	
			val requestBuilder = new RequestBuilder(request)
				.setMethod("GET")
				.setBodyEncoding(configuration.encoding)
				.setQueryParameters(null.asInstanceOf[FluentStringsMap])
				.setParameters(null.asInstanceOf[FluentStringsMap])
				.setUrl(redirectUrl)

			for (cookie <- CookieHandling.getStoredCookies(sessionWithUpdatedCookies, redirectUrl))
				requestBuilder.addOrReplaceCookie(cookie)

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
					logRequest(OK, response)
					executeNext(session, response)

				case phase :: otherPhases =>
					val phaseChecks = checks.filter(_.phase == phase)
					var (newSession, checkResult) = applyChecks(session, response, phaseChecks)

					checkResult match {
						case Failure(errorMessage) =>
							if (isDebugEnabled)
								debug(new StringBuilder().append("Check on request '").append(requestName).append("' failed : ").append(errorMessage).append(", response was:").append(response.dump))
							else
								warn(new StringBuilder().append("Check on request '").append(requestName).append("' failed : ").append(errorMessage))

							logRequest(KO, response, Some(errorMessage))
							executeNext(newSession, response)

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
	private def extractExtraInfo(response: ExtendedResponse): List[String] = {

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

		def extractExtraResponseInfo(protocolConfiguration: Option[HttpProtocolConfiguration], response: ExtendedResponse): List[String] = {

			if (response.isBuilt) {
				val extracted = try {
					for (
						httpProtocolConfig <- protocolConfiguration;
						extractor <- httpProtocolConfig.extraResponseInfoExtractor
					) yield extractor(response)

				} catch {
					case e: Exception =>
						warn("Encountered error while extracting extra response info", e)
						None
				}

				extracted.getOrElse(Nil)
			} else
				Nil
		}

		val extraRequestInfo = extractExtraRequestInfo(protocolConfiguration, request)
		val extraResponseInfo = extractExtraResponseInfo(protocolConfiguration, response)
		extraRequestInfo ::: extraResponseInfo
	}
}
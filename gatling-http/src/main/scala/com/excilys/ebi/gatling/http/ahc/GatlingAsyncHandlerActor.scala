/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.duration.DurationInt

import com.excilys.ebi.gatling.core.action.BaseActor
import com.excilys.ebi.gatling.core.check.Checks
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.{ KO, OK, RequestStatus }
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE
import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis
import com.excilys.ebi.gatling.core.validation.{ Failure, Success }
import com.excilys.ebi.gatling.http.Headers.{ Names => HeaderNames }
import com.excilys.ebi.gatling.http.cache.CacheHandling
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.cookie.CookieHandling
import com.excilys.ebi.gatling.http.request.ExtendedRequest
import com.excilys.ebi.gatling.http.response.{ ExtendedResponse, ExtendedResponseBuilder, ExtendedResponseBuilderFactory }
import com.excilys.ebi.gatling.http.util.HttpHelper.computeRedirectUrl
import com.ning.http.client.{ FluentStringsMap, Request, RequestBuilder }

import akka.actor.{ ActorRef, ReceiveTimeout }

object GatlingAsyncHandlerActor {
	val REDIRECTED_REQUEST_NAME_PATTERN = """(.+?) Redirect (\d+)""".r
	val REDIRECT_STATUS_CODES = 301 to 303

	def newAsyncHandlerActorFactory(
		checks: List[HttpCheck],
		next: ActorRef,
		protocolConfiguration: HttpProtocolConfiguration)(requestName: String) = {

		val handlerFactory = GatlingAsyncHandler.newHandlerFactory(checks, protocolConfiguration)
		val responseBuilderFactory = ExtendedResponseBuilder.newExtendedResponseBuilder(checks, protocolConfiguration)

		(request: Request, session: Session) =>
			new GatlingAsyncHandlerActor(
				session,
				checks,
				next,
				requestName,
				request,
				protocolConfiguration,
				handlerFactory,
				responseBuilderFactory)
	}
}

class GatlingAsyncHandlerActor(
	var originalSession: Session,
	checks: List[HttpCheck],
	next: ActorRef,
	var requestName: String,
	var request: Request,
	protocolConfiguration: HttpProtocolConfiguration,
	handlerFactory: HandlerFactory,
	responseBuilderFactory: ExtendedResponseBuilderFactory) extends BaseActor {

	var responseBuilder = responseBuilderFactory(request, originalSession)

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
			logRequest(originalSession, KO, response, Some(errorMessage))
			executeNext(originalSession.setFailed, response)

		case ReceiveTimeout =>
			val response = responseBuilder.build
			logRequest(originalSession, KO, response, Some("GatlingAsyncHandlerActor timed out"))
			executeNext(originalSession.setFailed, response)
	}

	def resetTimeout = context.setReceiveTimeout(configuration.http.requestTimeOutInMs milliseconds)

	private def logRequest(
		session: Session,
		requestStatus: RequestStatus,
		response: ExtendedResponse,
		errorMessage: Option[String] = None) {

		def dump = {
			val buff = new StringBuilder
			buff.append(END_OF_LINE).append(">>>>>>>>>>>>>>>>>>>>>>>>>>").append(END_OF_LINE)
			buff.append("request was:").append(END_OF_LINE)
			request.dumpTo(buff)
			buff.append("=========================").append(END_OF_LINE)
			buff.append("response was:").append(END_OF_LINE)
			response.dumpTo(buff)
			buff.append(END_OF_LINE).append("<<<<<<<<<<<<<<<<<<<<<<<<<")
			buff
		}

		/**
		 * Extract extra info from both request and response.
		 *
		 * @param response is the response to extract data from; request is retrieved from the property
		 * @return the extracted Strings
		 */
		def extraInfo: List[Any] =
			try {
				protocolConfiguration.extraInfoExtractor.map(_(requestStatus, session, request, response)).getOrElse(Nil)
			} catch {
				case e: Exception =>
					warn("Encountered error while extracting extra request info", e)
					Nil
			}

		if (requestStatus == KO) {
			warn(s"Request '$requestName' failed : ${errorMessage.getOrElse("")}")
			if (!isTraceEnabled) debug(dump)
		}
		trace(dump)

		DataWriter.logRequest(session.scenarioName, session.userId, requestName,
			response.executionStartDate, response.requestSendingEndDate, response.responseReceivingStartDate, response.executionEndDate,
			requestStatus, errorMessage, extraInfo)
	}

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param newSession the new Session
	 */
	private def executeNext(newSession: Session, response: ExtendedResponse) {
		next ! newSession.increaseTimeShift(nowMillis - response.executionEndDate)
		context.stop(self)
	}

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(response: ExtendedResponse) {

		def redirect(sessionWithUpdatedCookies: Session) {

			logRequest(originalSession, OK, response)

			val redirectUrl = computeRedirectUrl(response.getHeader(HeaderNames.LOCATION), request.getUrl)

			val requestBuilder = new RequestBuilder(request)
				.setMethod("GET")
				.setBodyEncoding(configuration.simulation.encoding)
				.setQueryParameters(null.asInstanceOf[FluentStringsMap])
				.setParameters(null.asInstanceOf[FluentStringsMap])
				.setUrl(redirectUrl)
				.setConnectionPoolKeyStrategy(request.getConnectionPoolKeyStrategy)

			for (cookie <- CookieHandling.getStoredCookies(sessionWithUpdatedCookies, redirectUrl))
				requestBuilder.addOrReplaceCookie(cookie)

			val newRequest = requestBuilder.build
			newRequest.getHeaders.remove(HeaderNames.CONTENT_LENGTH)
			newRequest.getHeaders.remove(HeaderNames.CONTENT_TYPE)

			val newRequestName = requestName match {
				case GatlingAsyncHandlerActor.REDIRECTED_REQUEST_NAME_PATTERN(requestBaseName, redirectCount) => requestBaseName + " Redirect " + (redirectCount.toInt + 1)
				case _ => requestName + " Redirect 1"
			}

			this.originalSession = sessionWithUpdatedCookies
			this.requestName = newRequestName
			this.request = newRequest
			this.responseBuilder = responseBuilderFactory(newRequest, originalSession)

			GatlingHttpClient.client.executeRequest(newRequest, handlerFactory(newRequestName, self))
		}

		val sessionWithUpdatedCookies = CookieHandling.storeCookies(originalSession, response.getUri, response.getCookies.toList)

		if (GatlingAsyncHandlerActor.REDIRECT_STATUS_CODES.contains(response.getStatusCode) && protocolConfiguration.followRedirectEnabled)
			redirect(sessionWithUpdatedCookies)

		else {
			val sessionWithUpdatedCache = CacheHandling.cache(protocolConfiguration, sessionWithUpdatedCookies, request, response)
			val checkResult = Checks.check(response, sessionWithUpdatedCache, checks)

			checkResult match {
				case Success(newSession) =>
					logRequest(newSession, OK, response)
					executeNext(newSession, response)

				case Failure(errorMessage) =>
					logRequest(sessionWithUpdatedCache, KO, response, Some(errorMessage))
					executeNext(sessionWithUpdatedCache, response)
			}
		}
	}
}
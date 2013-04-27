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

import com.ning.http.client.{ FluentStringsMap, Request, RequestBuilder }

import akka.actor.{ ActorRef, ReceiveTimeout }
import io.gatling.core.action.BaseActor
import io.gatling.core.check.Checks
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{ KO, OK, RequestMessage, Status }
import io.gatling.core.result.writer.DataWriter
import io.gatling.core.session.Session
import io.gatling.core.util.StringHelper.eol
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Failure, Success }
import io.gatling.http.Headers.{ Names => HeaderNames }
import io.gatling.http.cache.CacheHandling
import io.gatling.http.check.HttpCheck
import io.gatling.http.config.HttpProtocolConfiguration
import io.gatling.http.cookie.CookieHandling
import io.gatling.http.response.{ Response, ResponseBuilder, ResponseBuilderFactory, ResponseProcessor }
import io.gatling.http.util.{ HttpHelper, HttpStringBuilder }

object GatlingAsyncHandlerActor {
	val redirectedRequestNamePattern = """(.+?) Redirect (\d+)""".r
	val timeout = configuration.http.requestTimeOutInMs milliseconds

	def newAsyncHandlerActorFactory(
		checks: List[HttpCheck],
		next: ActorRef,
		responseProcessor: Option[ResponseProcessor],
		protocolConfiguration: HttpProtocolConfiguration)(requestName: String) = {

		val handlerFactory = GatlingAsyncHandler.newHandlerFactory(checks, protocolConfiguration)
		val responseBuilderFactory = ResponseBuilder.newResponseBuilder(checks, responseProcessor, protocolConfiguration)

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
	responseBuilderFactory: ResponseBuilderFactory) extends BaseActor {

	var responseBuilder = responseBuilderFactory(request)

	override def preStart {
		context.setReceiveTimeout(GatlingAsyncHandlerActor.timeout)
	}

	def receive = {
		case OnHeaderWriteCompleted(nanos) =>
			responseBuilder.updateRequestSendingEndDate(nanos)

		case OnContentWriteCompleted(nanos) =>
			responseBuilder.updateRequestSendingEndDate(nanos)

		case OnStatusReceived(status, nanos) =>
			responseBuilder.updateResponseReceivingStartDate(nanos)
			responseBuilder.accumulate(status)

		case OnHeadersReceived(headers) =>
			responseBuilder.accumulate(headers)

		case OnBodyPartReceived(bodyPart) =>
			responseBuilder.accumulate(bodyPart)

		case OnCompleted(nanos) =>
			responseBuilder.computeExecutionEndDateFromNanos(nanos)
			processResponse(responseBuilder.build)

		case OnThrowable(errorMessage, nanos) =>
			ko(originalSession, responseBuilder.computeExecutionEndDateFromNanos(nanos).build, errorMessage)

		case ReceiveTimeout =>
			ko(originalSession, responseBuilder.build, "GatlingAsyncHandlerActor timed out")
	}

	private def logRequest(
		session: Session,
		status: Status,
		response: Response,
		errorMessage: Option[String] = None) {

		def dump = {
			val buff = new StringBuilder
			buff.append(eol).append(">>>>>>>>>>>>>>>>>>>>>>>>>>").append(eol)
			buff.append("request was:").append(eol)
			buff.appendAHCRequest(request)
			buff.append("=========================").append(eol)
			buff.append("response was:").append(eol)
			buff.appendResponse(response)
			buff.append(eol).append("<<<<<<<<<<<<<<<<<<<<<<<<<")
			buff.toString
		}

		/**
		 * Extract extra info from both request and response.
		 *
		 * @param response is the response to extract data from; request is retrieved from the property
		 * @return the extracted Strings
		 */
		def extraInfo: List[Any] =
			try {
				protocolConfiguration.extraInfoExtractor.map(_(status, session, request, response)).getOrElse(Nil)
			} catch {
				case e: Exception =>
					logger.warn("Encountered error while extracting extra request info", e)
					Nil
			}

		if (status == KO) {
			logger.warn(s"Request '$requestName' failed : ${errorMessage.getOrElse("")}")
			if (!logger.underlying.isTraceEnabled) logger.debug(dump)
		}
		logger.trace(dump)

		DataWriter.tell(RequestMessage(session.scenarioName, session.userId, session.groupStack, requestName,
			response.executionStartDate, response.requestSendingEndDate, response.responseReceivingStartDate, response.executionEndDate,
			status, errorMessage, extraInfo))
	}

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param newSession the new Session
	 */
	private def executeNext(newSession: Session, response: Response) {
		next ! newSession.increaseTimeShift(nowMillis - response.executionEndDate)
		context.stop(self)
	}

	private def ok(session: Session, response: Response) {
		logRequest(session, OK, response, None)
		executeNext(session, response)
	}

	private def ko(session: Session, response: Response, message: String) {
		val failedSession = session.fail
		logRequest(failedSession, KO, response, Some(message))
		executeNext(failedSession, response)
	}

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(response: Response) {

		def redirect(sessionWithUpdatedCookies: Session) {

			logRequest(originalSession, OK, response)

			val redirectUrl = HttpHelper.computeRedirectUrl(response.getHeader(HeaderNames.LOCATION), request.getUrl)

			val requestBuilder = new RequestBuilder(request)
				.setMethod("GET")
				.setBodyEncoding(configuration.core.encoding)
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
				case GatlingAsyncHandlerActor.redirectedRequestNamePattern(requestBaseName, redirectCount) => requestBaseName + " Redirect " + (redirectCount.toInt + 1)
				case _ => requestName + " Redirect 1"
			}

			this.originalSession = sessionWithUpdatedCookies
			this.requestName = newRequestName
			this.request = newRequest
			this.responseBuilder = responseBuilderFactory(newRequest)

			GatlingHttpClient.client.executeRequest(newRequest, handlerFactory(newRequestName, self))
		}

		def checkAndProceed(sessionWithUpdatedCookies: Session) {
			val sessionWithUpdatedCache = CacheHandling.cache(protocolConfiguration, sessionWithUpdatedCookies, request, response)
			val checkResult = Checks.check(response, sessionWithUpdatedCache, checks)

			checkResult match {
				case Success(newSession) => ok(newSession, response)
				case Failure(errorMessage) => ko(sessionWithUpdatedCache, response, errorMessage)
			}
		}

		val sessionWithUpdatedCookies = CookieHandling.storeCookies(originalSession, response.getUri, response.getCookies.toList)

		if (response.isRedirected && protocolConfiguration.followRedirectEnabled)
			redirect(sessionWithUpdatedCookies)
		else
			checkAndProceed(sessionWithUpdatedCookies)
	}
}
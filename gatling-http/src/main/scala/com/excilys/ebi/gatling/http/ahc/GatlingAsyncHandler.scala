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

import java.lang.Void
import java.util.concurrent.TimeUnit

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.immutable.HashMap
import scala.collection.mutable.{HashMap => MHashMap}

import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.check.extractor.{MultiValuedExtractor, ExtractorFactory, Extractor}
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ResultStatus.{ResultStatus, OK, KO}
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.Predef.SET_COOKIE
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.request.HttpPhase.{HttpPhase, CompletePageReceived}
import com.excilys.ebi.gatling.http.request.HttpPhase
import com.excilys.ebi.gatling.http.util.HttpHelper.COOKIES_CONTEXT_KEY
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.Response.ResponseBuilder
import com.ning.http.client.{Response, HttpResponseStatus, HttpResponseHeaders, HttpResponseBodyPart, Cookie, AsyncHandler}
import com.ning.http.util.AsyncHttpProviderUtils.parseCookie

import akka.actor.Actor.registry.actorFor

/**
 * This class is the AsyncHandler that AsyncHttpClient needs to process a request's response
 *
 * It is part of the HttpRequestAction
 *
 * @constructor constructs a GatlingAsyncHandler
 * @param session the session of the scenario
 * @param checks the checks that will be done on response
 * @param next the next action to be executed
 * @param requestName the name of the request
 */
class GatlingAsyncHandler(session: Session, checks: List[HttpCheck], next: Action, requestName: String)
		extends AsyncHandler[Void] with Logging {

	private val executionStartTimeNano = System.nanoTime

	private val executionStartDate = DateTime.now

	private val identifier = requestName + session.userId

	private val responseBuilder = new ResponseBuilder

	def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {
		responseBuilder.accumulate(responseStatus)
		STATE.CONTINUE
	}

	def onHeadersReceived(headers: HttpResponseHeaders): STATE = {

		def handleCookies(headers: HttpResponseHeaders) {
			val headersMap = headers.getHeaders

			val setCookieHeaders = headersMap.get(SET_COOKIE)
			if (setCookieHeaders != null) {
				var sessionCookies = session.getAttributeAsOption[HashMap[String, Cookie]](COOKIES_CONTEXT_KEY).getOrElse(HashMap.empty)

				setCookieHeaders.foreach { setCookieHeader =>
					val cookie = parseCookie(setCookieHeader)
					sessionCookies += (cookie.getName -> cookie)
				}

				logger.debug("Cookies put in Session: {}", sessionCookies)

				session setAttribute (COOKIES_CONTEXT_KEY, sessionCookies)
			}
		}

		responseBuilder.accumulate(headers)
		handleCookies(headers)

		STATE.CONTINUE
	}

	def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
		// only store bodyparts if they are to be analyzed
		if (!getChecksForPhase(CompletePageReceived).isEmpty) {
			responseBuilder.accumulate(bodyPart)
		}
		STATE.CONTINUE
	}

	def onCompleted(): Void = {
		val response = responseBuilder.build
		processResponse(response)
		null
	}

	def onThrowable(throwable: Throwable) = {
		logger.error("{}\n{}", throwable.getClass, throwable.getStackTraceString)
		sendLogAndExecuteNext(KO, throwable.getMessage, System.nanoTime)
	}

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param requestResult the result of the request
	 * @param requestMessage the message that will be logged
	 * @param processingStartTimeNano date of the beginning of the response processing
	 */
	private def sendLogAndExecuteNext(requestResult: ResultStatus, requestMessage: String, processingStartTimeNano: Long) = {
		actorFor(session.writeActorUuid).map { a =>
			val responseTimeMillis = TimeUnit.MILLISECONDS.convert(processingStartTimeNano - executionStartTimeNano, TimeUnit.NANOSECONDS)
			a ! ActionInfo(session.scenarioName, session.userId, "Request " + requestName, executionStartDate, responseTimeMillis, requestResult, requestMessage)
		}

		session.setAttribute(Session.LAST_ACTION_DURATION_KEY, System.nanoTime() - processingStartTimeNano)

		next.execute(session)
	}

	private def getChecksForPhase(httpPhase: HttpPhase) = checks.view.filter(_.when == httpPhase)

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(response: Response) {

		/**
		 * This method instantiate the required extractors
		 *
		 * @param checks the checks that were given for this response
		 * @param response the response on which the checks will be made
		 */
		def prepareExtractors(checks: Iterable[HttpCheck], response: Response): MHashMap[ExtractorFactory[Response], Extractor] = {

			val extractors: MHashMap[ExtractorFactory[Response], Extractor] = MHashMap.empty
			checks.foreach { check =>
				val extractorFactory = check.how
				if (extractors.get(extractorFactory).isEmpty)
					extractors += extractorFactory -> extractorFactory.getExtractor(response)
			}

			extractors
		}

		val processingStartTimeNano = System.nanoTime

		HttpPhase.values.foreach { httpPhase =>

			val phaseChecks = getChecksForPhase(httpPhase)
			if (!phaseChecks.isEmpty) {

				val phaseExtractors = prepareExtractors(phaseChecks, response)

				for (check <- phaseChecks) {
					val extractor = phaseExtractors.get(check.how).get
					val expression = check.what(session)
					val extractedValue = extractor.extract(expression)
					logger.debug("Extracted value: {}", extractedValue)

					if (!check.check(extractedValue, session)) {
						if (logger.isWarnEnabled)
							logger.warn("{} failed : received '{}' instead of '{}' for '{}'", Array[Object](check, extractedValue, check.expected, expression))

						sendLogAndExecuteNext(KO, check + " failed", processingStartTimeNano)
						return

					} else if (!extractedValue.isEmpty && check.saveAs.isDefined) {
						session.setAttribute(check.saveAs.get, extractor match {
							case multi: MultiValuedExtractor => extractedValue
							case single => extractedValue(0)
						})
					}
				}
			}
		}

		sendLogAndExecuteNext(OK, "Request Executed Successfully", processingStartTimeNano)
	}
}
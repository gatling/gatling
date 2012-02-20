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
import java.lang.Void
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.immutable.HashMap
import com.excilys.ebi.gatling.core.check.Check.applyChecks
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ResultStatus.{ ResultStatus, OK, KO }
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.Predef.SET_COOKIE
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.request.HttpPhase.{ HttpPhase, CompletePageReceived }
import com.excilys.ebi.gatling.http.request.HttpPhase
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.Response.ResponseBuilder
import com.ning.http.client.{ ProgressAsyncHandler, Response, HttpResponseStatus, HttpResponseHeaders, HttpResponseBodyPart, Cookie, AsyncHandler }
import com.ning.http.util.AsyncHttpProviderUtils.parseCookie
import akka.actor.ActorRef
import com.excilys.ebi.gatling.http.cookie.CookieHandling
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

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
class GatlingAsyncHandler(session: Session, checks: List[HttpCheck[_]], next: ActorRef, requestName: String)
		extends AsyncHandler[Void] with ProgressAsyncHandler[Void] with CookieHandling with Logging {

	private val identifier = requestName + session.userId

	private val responseBuilder = new ResponseBuilder

	private val requestStartDate = currentTimeMillis

	// initialized in case the headers can't be sent (connection problem)
	@volatile private var endOfRequestSendingDate: Option[Long] = None

	// start of response or exception
	@volatile private var startOfResponseReceivingDate: Option[Long] = None

	// end of response or exception
	@volatile private var responseEndDate: Option[Long] = None

	def onHeaderWriteCompleted = {
		endOfRequestSendingDate = Some(currentTimeMillis)
		STATE.CONTINUE
	}

	def onContentWriteCompleted = {
		// reassign in case of request body
		endOfRequestSendingDate = Some(currentTimeMillis)
		STATE.CONTINUE
	}

	def onContentWriteProgress(amount: Long, current: Long, total: Long) = STATE.CONTINUE

	def onStatusReceived(responseStatus: HttpResponseStatus) = {
		startOfResponseReceivingDate = Some(currentTimeMillis)
		responseBuilder.accumulate(responseStatus)
		STATE.CONTINUE
	}

	def onHeadersReceived(headers: HttpResponseHeaders) = {
		responseBuilder.accumulate(headers)
		STATE.CONTINUE
	}

	def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
		// only store bodyparts if they are to be analyzed
		if (!getChecksForPhase(CompletePageReceived).isEmpty) {
			responseBuilder.accumulate(bodyPart)
		}
		STATE.CONTINUE
	}

	def onCompleted: Void = {
		responseEndDate = Some(currentTimeMillis)
		processResponse(responseBuilder.build)
		null
	}

	def onThrowable(throwable: Throwable) {
		logger.warn("Request '" + requestName + "' failed", throwable)
		val errorMessage = Option(throwable.getMessage) match {
			case Some(message) => message
			case None => EMPTY
		}
		sendLogAndExecuteNext(session, KO, errorMessage)
	}

	/**
	 * This method is used to send a message to the data writer actor and then execute the next action
	 *
	 * @param session the new Session
	 * @param requestResult the result of the request
	 * @param requestMessage the message that will be logged
	 * @param processingStartDate date of the beginning of the response processing
	 */
	private def sendLogAndExecuteNext(newSession: Session, requestResult: ResultStatus, requestMessage: String) {

		val now = currentTimeMillis
		val effectiveResponseEndDate = responseEndDate.getOrElse(now)
		val effectiveEndOfRequestSendingDate = endOfRequestSendingDate.getOrElse(now)
		val effectivestartOfResponseReceivingDate = startOfResponseReceivingDate.getOrElse(now)
		DataWriter.instance ! ActionInfo(session.scenarioName, session.userId, "Request " + requestName, requestStartDate, effectiveResponseEndDate, effectiveEndOfRequestSendingDate, effectivestartOfResponseReceivingDate, requestResult, requestMessage)

		next ! newSession.setAttribute(Session.LAST_ACTION_DURATION_KEY, currentTimeMillis - effectiveResponseEndDate)
	}

	def getChecksForPhase(httpPhase: HttpPhase) = checks.filter(_.phase == httpPhase)

	/**
	 * This method processes the response if needed for each checks given by the user
	 */
	private def processResponse(response: Response) {

		var newSession = storeCookies(session, response.getUri.toString, response.getCookies)

		HttpPhase.values.foreach { httpPhase =>
			val phaseChecks = getChecksForPhase(httpPhase)
			if (!phaseChecks.isEmpty) {
				var (newSessionWithSavedValues, checkResult) = applyChecks(newSession, response, phaseChecks)
				newSession = newSessionWithSavedValues

				if (!checkResult.ok) {
					val errorMessage = checkResult.errorMessage.getOrElse(throw new IllegalArgumentException("Missing error message"))
					if (logger.isWarnEnabled)
						logger.warn("Check on request '{}' failed : '{}'", requestName, errorMessage)

					sendLogAndExecuteNext(newSession, KO, errorMessage)
					return
				}
			}
		}

		sendLogAndExecuteNext(newSession, OK, "Request Executed Successfully")
	}
}
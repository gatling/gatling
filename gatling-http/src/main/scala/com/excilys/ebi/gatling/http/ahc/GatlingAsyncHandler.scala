/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.ahc

import java.util.concurrent.TimeUnit
import scala.collection.immutable.HashMap
import scala.collection.mutable.{Set => MSet, MultiMap, HashMap => MHashMap}
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE
import org.joda.time.DateTime
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ResultStatus.{ResultStatus, OK, KO}
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.request.HttpPhase.{HttpPhase, CompletePageReceived}
import com.excilys.ebi.gatling.http.request.HttpPhase
import com.excilys.ebi.gatling.http.util.GatlingHttpHelper.COOKIES_CONTEXT_KEY
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.Response.ResponseBuilder
import com.ning.http.client.{Response, HttpResponseStatus, HttpResponseHeaders, HttpResponseBodyPart, Cookie, AsyncHandler}
import com.ning.http.util.AsyncHttpProviderUtils.parseCookie
import akka.actor.Actor.registry.actorFor
import com.excilys.ebi.gatling.http.capture.HttpCapture
import com.excilys.ebi.gatling.core.capture.capturer.CapturerFactory
import com.excilys.ebi.gatling.core.capture.capturer.Capturer
import com.excilys.ebi.gatling.http.capture.check.HttpCheck

class GatlingAsyncHandler(context: Context, processors: MSet[HttpCapture], next: Action, requestName: String, groups: List[String]) extends AsyncHandler[Void] with Logging {

	private val executionStartTimeNano = System.nanoTime

	private val executionStartDate = DateTime.now()

	private val indexedProcessors: MultiMap[HttpPhase, HttpCapture] = new MHashMap[HttpPhase, MSet[HttpCapture]] with MultiMap[HttpPhase, HttpCapture]
	processors.foreach(processor => indexedProcessors.addBinding(processor.when, processor))

	private val identifier = requestName + context.getUserId

	private val responseBuilder = new ResponseBuilder()

	def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {
		responseBuilder.accumulate(responseStatus)
		STATE.CONTINUE
	}

	def onHeadersReceived(headers: HttpResponseHeaders): STATE = {

		def handleCookies(headers: HttpResponseHeaders) {
			val headersMap = headers.getHeaders

			val setCookieHeaders = headersMap.get(SET_COOKIE)
			if (setCookieHeaders != null) {
				var contextCookies = context.getAttributeAsOption(COOKIES_CONTEXT_KEY).getOrElse(HashMap.empty).asInstanceOf[HashMap[String, Cookie]]

				val it = setCookieHeaders.iterator
				while (it.hasNext) {
					val cookie = parseCookie(it.next)
					contextCookies += (cookie.getName -> cookie)
				}

				logger.debug("Cookies put in Context: {}", contextCookies)

				context setAttribute (COOKIES_CONTEXT_KEY, contextCookies)
			}
		}

		responseBuilder.accumulate(headers)
		handleCookies(headers)

		STATE.CONTINUE
	}

	def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
		// only store bodyparts if they are to be analyzed
		if (isPhaseToBeProcessed(CompletePageReceived)) {
			responseBuilder.accumulate(bodyPart)
		}
		STATE.CONTINUE
	}

	private def sendLogAndExecuteNext(requestResult: ResultStatus, requestMessage: String, processingStartTimeNano: Option[Long]) = {
		actorFor(context.getWriteActorUuid).map { a =>
			val responseTimeMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime - executionStartTimeNano, TimeUnit.NANOSECONDS)
			a ! ActionInfo(context.getScenarioName, context.getUserId, "Request " + requestName, executionStartDate, responseTimeMillis, requestResult, requestMessage, groups)
		}

		if (processingStartTimeNano.isDefined) {
			context.setAttribute(Context.LAST_ACTION_DURATION_KEY, System.nanoTime() - processingStartTimeNano.get)
		} else {
			context.removeAttribute(Context.LAST_ACTION_DURATION_KEY);
		}

		logger.debug("Context Cookies sent to next action: {}", context.getAttributeAsOption(COOKIES_CONTEXT_KEY).getOrElse(HashMap.empty))
		next.execute(context)
	}

	private def isPhaseToBeProcessed(httpPhase: HttpPhase) = indexedProcessors.get(httpPhase).isDefined

	private def processResponse(response: Response) {

		def prepareProviders(processors: MSet[HttpCapture], response: Response): MHashMap[CapturerFactory[Response], Capturer] = {

			val providers: MHashMap[CapturerFactory[Response], Capturer] = MHashMap.empty
			processors.foreach { processor =>
				val providerFactory = processor.how
				if (providers.get(providerFactory).isEmpty)
					providers += providerFactory -> providerFactory.getCapturer(response)
			}

			providers
		}

		val processingStartTimeNano = System.nanoTime

		HttpPhase.values.foreach { httpPhase =>

			if (isPhaseToBeProcessed(httpPhase)) {
				val phaseProcessors = indexedProcessors.get(httpPhase).get

				val phaseProviders = prepareProviders(phaseProcessors, response)

				for (processor <- phaseProcessors) {
					processor match {
						case c: HttpCapture =>
							val provider = phaseProviders.get(processor.how).get
							val value = provider.capture(c.what.apply(context))
							logger.debug("Captured Value: {}", value)
							
							if (c.isInstanceOf[HttpCheck] && !c.asInstanceOf[HttpCheck].getResult(value)) {
								logger.warn("CHECK RESULT: false expected {} but received {}", c, value)
								sendLogAndExecuteNext(KO, c + " failed", Some(processingStartTimeNano))
								return

							} else if (!value.isDefined) {
								logger.warn("Capture {} could not get value required by user", c)
								sendLogAndExecuteNext(KO, c + " failed", Some(processingStartTimeNano))
								return

							} else if (c.to != EMPTY) {
								context.setAttribute(c.to, value.get.toString)
							}

						case _ => throw new IllegalArgumentException
					}
				}
			}
		}

		sendLogAndExecuteNext(OK, "Request Executed Successfully", Some(processingStartTimeNano))
	}

	def onCompleted(): Void = {
		val response = responseBuilder.build
		processResponse(response)
		null
	}

	def onThrowable(throwable: Throwable) = {
		logger.error("{}\n{}", throwable.getClass, throwable.getStackTraceString)
		sendLogAndExecuteNext(KO, throwable.getMessage, None)
	}
}
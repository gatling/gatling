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

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit

import scala.collection.immutable.HashMap
import scala.collection.mutable.{Set => MSet, HashMap => MHashMap, MultiMap}

import org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE
import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider
import com.excilys.ebi.gatling.core.provider.ProviderType
import com.excilys.ebi.gatling.core.result.message.ResultStatus.{ResultStatus, OK, KO}
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.request.HttpPhase.{StatusReceived, HttpPhase, HeadersReceived, CompletePageReceived}
import com.excilys.ebi.gatling.http.util.GatlingHttpHelper.COOKIES_CONTEXT_KEY
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.Response.ResponseBuilder
import com.ning.http.client.{Response, HttpResponseStatus, HttpResponseHeaders, HttpResponseBodyPart, Cookie, AsyncHandler}
import com.ning.http.util.AsyncHttpProviderUtils.parseCookie

import akka.actor.Actor.registry.actorFor

class CustomAsyncHandler(context: Context, processors: MSet[HttpProcessor], next: Action, requestName: String, groups: List[String]) extends AsyncHandler[Response] with Logging {

	private val executionStartTimeNano = System.nanoTime

	private val executionStartDate = DateTime.now()
	
	private var processingStartTimeNano = executionStartTimeNano

	private val indexedProcessors: MultiMap[HttpPhase, HttpProcessor] = new MHashMap[HttpPhase, MSet[HttpProcessor]] with MultiMap[HttpPhase, HttpProcessor]
	processors.foreach(processor => indexedProcessors.addBinding(processor.getHttpPhase, processor))

	private val identifier = requestName + context.getUserId

	private val responseBuilder: ResponseBuilder = new ResponseBuilder()

	private val hasSentLog = new AtomicBoolean(false)

	private def isPhaseToBeProcessed(httpPhase: HttpPhase): Boolean = {
		(indexedProcessors.get(httpPhase).isDefined && !hasSentLog.get()) || httpPhase == HeadersReceived
	}

	private def sendLogAndExecuteNext(requestResult: ResultStatus, requestMessage: String) = {
		if (hasSentLog.compareAndSet(false, true)) {
			actorFor(context.getWriteActorUuid).map { a =>
				val responseTimeMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime - executionStartTimeNano, TimeUnit.NANOSECONDS)
				a ! ActionInfo(context.getScenarioName, context.getUserId, "Request " + requestName, executionStartDate, responseTimeMillis, requestResult, requestMessage, groups)
			}

			context.setAttribute(Context.LAST_ACTION_DURATION_KEY, System.nanoTime() - processingStartTimeNano)

			logger.debug("Context Cookies sent to next action: {}", context.getAttributeAsOption(COOKIES_CONTEXT_KEY).getOrElse(HashMap.empty))
			next.execute(context)
		}
	}

	private def processResponse(httpPhase: HttpPhase, placeToSearch: Any) {

		def prepareProviders(processors: MSet[HttpProcessor], placeToSearch: Any): MHashMap[ProviderType, AbstractCaptureProvider] = {

			val providers: MHashMap[ProviderType, AbstractCaptureProvider] = MHashMap.empty
			processors.foreach { processor =>
				val providerType = processor.getProviderType
				if (providers.get(providerType).isEmpty)
					providers += (providerType -> providerType.getProvider(placeToSearch))
			}

			providers
		}

		def getPreparedProvider(processor: HttpCapture, providers: MHashMap[ProviderType, AbstractCaptureProvider]) = providers.get(processor.getProviderType).getOrElse(throw new IllegalArgumentException);

		def captureData(processor: HttpCapture, provider: AbstractCaptureProvider) = provider.capture(processor.expressionFormatter.apply(context))

		if (isPhaseToBeProcessed(httpPhase)) {
			val phaseProcessors = indexedProcessors.get(httpPhase).get

			val providers = prepareProviders(phaseProcessors, placeToSearch)

			for (processor <- phaseProcessors) {
				processor match {
					case c: HttpCapture =>
						val provider = getPreparedProvider(c, providers)
						val value = captureData(c, provider)
						logger.debug("Captured Value: {}", value)

						// Is the value what is expected ?
						val isResultValid =
							if (c.isInstanceOf[HttpCheck])
								c.asInstanceOf[HttpCheck].getResult(value)
							else
								value.isDefined

						if (isResultValid) {
							if (c.getAttrKey != EMPTY && value.isDefined)
								context setAttribute (c.getAttrKey, value.get.toString)
						} else {
							if (c.isInstanceOf[HttpCheck])
								logger.warn("CHECK RESULT: false expected {} but received {}", c, value)
							else
								logger.warn("Capture {} could not get value required by user", c)
							sendLogAndExecuteNext(KO, c + " failed")
						}

					case _ => throw new IllegalArgumentException
				}
			}
		}
	}

	def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {

		processingStartTimeNano = System.nanoTime()

		if (isPhaseToBeProcessed(StatusReceived)) {
			responseBuilder.accumulate(responseStatus)
			processResponse(StatusReceived, responseStatus.getStatusCode)
		}
		STATE.CONTINUE
	}

	def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
		if (isPhaseToBeProcessed(HeadersReceived)) {
			responseBuilder.accumulate(headers)

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

			processResponse(HeadersReceived, headersMap)
		}
		STATE.CONTINUE
	}

	def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
		if (isPhaseToBeProcessed(CompletePageReceived)) {
			responseBuilder.accumulate(bodyPart)
		}
		STATE.CONTINUE
	}

	def onCompleted(): Response = {
		if (isPhaseToBeProcessed(CompletePageReceived)) {
			val response = responseBuilder.build
			processResponse(CompletePageReceived, response)
		}
		sendLogAndExecuteNext(OK, "Request Executed Successfully")
		null
	}

	def onThrowable(throwable: Throwable) = {
		logger.error("{}\n{}", throwable.getClass, throwable.getStackTraceString)
		sendLogAndExecuteNext(KO, throwable.getMessage)
	}

xt(KO, throwable.getMessage)
	}

}
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

import scala.collection.mutable.{ MultiMap, HashMap }
import scala.collection.immutable.{ HashSet, HashMap => IHashMap }
import scala.collection.{ Set => CSet }
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider
import com.excilys.ebi.gatling.core.provider.ProviderType
import com.excilys.ebi.gatling.core.result.message.ResultStatus._
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.util.GatlingHttpHelper._
import com.excilys.ebi.gatling.core.util.StringHelper._
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.Response.ResponseBuilder
import com.ning.http.client.AsyncHandler
import com.ning.http.client.HttpResponseBodyPart
import com.ning.http.client.HttpResponseHeaders
import com.ning.http.client.HttpResponseStatus
import com.ning.http.client.Response
import com.ning.http.client.FluentCaseInsensitiveStringsMap
import com.ning.http.client.Cookie
import com.ning.http.util.AsyncHttpProviderUtils._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import akka.actor.Actor.registry._
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.joda.time.DateTime

class CustomAsyncHandler(context: Context, processors: MultiMap[HttpPhase, HttpProcessor], next: Action, executionStartTimeNano: Long,
	executionStartDate: DateTime, requestName: String, groups: List[String])
		extends AsyncHandler[Response] with Logging {

	private val identifier = requestName + context.getUserId

	private val responseBuilder: ResponseBuilder = new ResponseBuilder()

	private val hasSentLog = new AtomicBoolean(false)

	private var processingStartTimeNano = System.nanoTime()

	private def isPhaseToBeProcessed(httpPhase: HttpPhase): Boolean = {
		(processors.get(httpPhase).isDefined && !hasSentLog.get()) || httpPhase == HeadersReceived
	}

	private def sendLogAndExecuteNext(requestResult: ResultStatus, requestMessage: String) = {
		if (hasSentLog.compareAndSet(false, true)) {
			actorFor(context.getWriteActorUuid).map { a =>
				val responseTimeMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime - executionStartTimeNano, TimeUnit.NANOSECONDS)
				a ! ActionInfo(context.getScenarioName, context.getUserId, "Request " + requestName, executionStartDate, responseTimeMillis, requestResult, requestMessage, groups)
			}

			context.setAttribute(Context.LAST_ACTION_DURATION_KEY, System.nanoTime() - processingStartTimeNano)

			logger.debug("Context Cookies sent to next action: {}", context.getAttributeAsOption(COOKIES_CONTEXT_KEY).getOrElse(IHashMap.empty))
			next.execute(context)
		}
	}

	private def processResponse(httpPhase: HttpPhase, placeToSearch: Any) {

		def prepareProviders(processors: CSet[HttpProcessor], placeToSearch: Any): HashMap[ProviderType, AbstractCaptureProvider] = {

			val providers: HashMap[ProviderType, AbstractCaptureProvider] = HashMap.empty
			processors.foreach { processor =>
				val providerType = processor.getProviderType
				if (providers.get(providerType).isEmpty)
					providers += (providerType -> providerType.getProvider(placeToSearch))
			}

			providers
		}

		def getPreparedProvider(processor: HttpCapture, providers: HashMap[ProviderType, AbstractCaptureProvider]) = providers.get(processor.getProviderType).getOrElse(throw new IllegalArgumentException);

		def captureData(processor: HttpCapture, provider: AbstractCaptureProvider) = provider.capture(processor.expressionFormatter.apply(context))

		if (isPhaseToBeProcessed(httpPhase)) {
			logger.debug("Processors at {} : {}", httpPhase, processors.get(httpPhase))

			val phaseProcessors = processors.get(httpPhase).getOrElse(new HashSet[HttpProcessor])

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
				var contextCookies = context.getAttributeAsOption(COOKIES_CONTEXT_KEY).getOrElse(IHashMap.empty).asInstanceOf[IHashMap[String, Cookie]]

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

}
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

import java.util.concurrent.atomic.AtomicBoolean

import com.ning.http.client.{ HttpResponseBodyPart, HttpResponseHeaders, HttpResponseStatus, ProgressAsyncHandler }
import com.ning.http.client.AsyncHandler.STATE.CONTINUE
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.http.response.ResponseBuilder

/**
 * This class is the AsyncHandler that AsyncHttpClient needs to process a request's response
 *
 * It is part of the HttpRequestAction
 *
 * @constructor constructs a GatlingAsyncHandler
 * @param task the data about the request to be sent and processed
 * @param responseBuilder the builder for the response
 */
class AsyncHandler(task: HttpTask) extends ProgressAsyncHandler[Unit] with Logging {

	val responseBuilder = task.responseBuilderFactory(task.request)
	private val done = new AtomicBoolean(false)

	def onHeaderWriteCompleted = {
		if (!done.get) responseBuilder.updateLastByteSent
		CONTINUE
	}

	def onContentWriteCompleted = {
		if (!done.get) responseBuilder.updateLastByteSent
		CONTINUE
	}

	def onContentWriteProgress(amount: Long, current: Long, total: Long) = CONTINUE

	def onStatusReceived(status: HttpResponseStatus) = {
		if (!done.get) responseBuilder.accumulate(status)
		CONTINUE
	}

	def onHeadersReceived(headers: HttpResponseHeaders) = {
		if (!done.get) responseBuilder.accumulate(headers)
		CONTINUE
	}

	def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
		if (!done.get) responseBuilder.accumulate(bodyPart)
		CONTINUE
	}

	def onCompleted {
		if (!done.getAndSet(true)) AsyncHandlerActor.asyncHandlerActor ! OnCompleted(task, responseBuilder.build)
	}

	def onThrowable(throwable: Throwable) {
		if (!done.getAndSet(true)) {
			val errorMessage = Option(throwable.getMessage).getOrElse(throwable.getClass.getName)
			if (logger.underlying.isInfoEnabled)
				logger.warn(s"Request '${task.requestName}' failed for user ${task.session.userId}", throwable)
			else
				logger.warn(s"Request '${task.requestName}' failed for user ${task.session.userId}: $errorMessage")
			AsyncHandlerActor.asyncHandlerActor ! OnThrowable(task, responseBuilder.updateLastByteReceived.build, errorMessage)
		}
	}
}
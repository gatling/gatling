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

import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.request.HttpPhase.{ CompletePageReceived, BodyPartReceived }
import com.ning.http.client.{ AsyncHandler, ProgressAsyncHandler, HttpResponseStatus, HttpResponseHeaders, HttpResponseBodyPart }
import com.ning.http.client.AsyncHandler.STATE.CONTINUE

import akka.actor.ActorRef
import grizzled.slf4j.Logging

object GatlingAsyncHandler {

	def newHandlerFactory(checks: List[HttpCheck[_]]): HandlerFactory = {
		val useBodyParts = checks.exists(check => check.phase == BodyPartReceived || check.phase == CompletePageReceived)
		(requestName: String, actor: ActorRef) => new GatlingAsyncHandler(requestName, actor, useBodyParts)
	}
}

/**
 * This class is the AsyncHandler that AsyncHttpClient needs to process a request's response
 *
 * It is part of the HttpRequestAction
 *
 * @constructor constructs a GatlingAsyncHandler
 * @param requestName the name of the request
 * @param actor the actor that will perform the logic outside of the IO thread
 * @param useBodyParts id body parts should be sent to the actor
 */
class GatlingAsyncHandler(requestName: String, actor: ActorRef, useBodyParts: Boolean)
		extends AsyncHandler[Void] with ProgressAsyncHandler[Void] with Logging {

	def onHeaderWriteCompleted = {
		actor ! new OnHeaderWriteCompleted
		CONTINUE
	}

	def onContentWriteCompleted = {
		actor ! new OnContentWriteCompleted
		CONTINUE
	}

	def onContentWriteProgress(amount: Long, current: Long, total: Long) = CONTINUE

	def onStatusReceived(responseStatus: HttpResponseStatus) = {
		actor ! new OnStatusReceived(responseStatus)
		CONTINUE
	}

	def onHeadersReceived(headers: HttpResponseHeaders) = {
		actor ! new OnHeadersReceived(headers)
		CONTINUE
	}

	def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
		actor ! new OnBodyPartReceived(if (useBodyParts) Some(bodyPart) else None)
		CONTINUE
	}

	def onCompleted: Void = {
		actor ! new OnCompleted
		null
	}

	def onThrowable(throwable: Throwable) {
		warn("Request '" + requestName + "' failed", throwable)
		val errorMessage = Option(throwable.getMessage).getOrElse(throwable.getClass.getName)
		actor ! new OnThrowable(errorMessage)
	}
}
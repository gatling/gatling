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

import java.lang.System.nanoTime

import com.ning.http.client.{ HttpResponseBodyPart, HttpResponseHeaders, HttpResponseStatus, Request }

import akka.actor.ActorRef
import io.gatling.core.session.Session
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.ResponseBuilderFactory

sealed trait HttpEvent

case class AsyncHandlerActorState(session: Session,
	request: Request,
	requestName: String,
	checks: List[HttpCheck],
	handlerFactory: HandlerFactory,
	responseBuilderFactory: ResponseBuilderFactory,
	next: ActorRef) extends HttpEvent
case class OnHeaderWriteCompleted(nanos: Long = nanoTime) extends HttpEvent
case class OnContentWriteCompleted(nanos: Long = nanoTime) extends HttpEvent
case class OnStatusReceived(responseStatus: HttpResponseStatus, nanos: Long = nanoTime) extends HttpEvent
case class OnHeadersReceived(headers: HttpResponseHeaders) extends HttpEvent
case class OnBodyPartReceived(bodyPart: Option[HttpResponseBodyPart]) extends HttpEvent
case class OnCompleted(nanos: Long = nanoTime) extends HttpEvent
case class OnThrowable(errorMessage: String, nanos: Long = nanoTime) extends HttpEvent
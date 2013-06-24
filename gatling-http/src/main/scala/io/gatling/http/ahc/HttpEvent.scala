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

import com.ning.http.client.Request

import akka.actor.ActorRef
import io.gatling.core.session.Session
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.{ Response, ResponseBuilderFactory }

sealed trait HttpEvent

case class AsyncHandlerActorState(session: Session,
	request: Request,
	requestName: String,
	checks: List[HttpCheck],
	responseBuilderFactory: ResponseBuilderFactory,
	next: ActorRef) extends HttpEvent
case class OnCompleted(response: Response) extends HttpEvent
case class OnThrowable(response: Response, errorMessage: String) extends HttpEvent
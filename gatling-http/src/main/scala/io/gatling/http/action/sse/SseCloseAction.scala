/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.sse

import akka.actor.{ Props, ActorRef }
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.Validation
import io.gatling.http.action.RequestAction

object SseCloseAction {
  def props(requestName: Expression[String], sseName: String, dataWriters: DataWriters, next: ActorRef) =
    Props(new SseCloseAction(requestName, sseName, dataWriters, next))
}

class SseCloseAction(val requestName: Expression[String], sseName: String, dataWriters: DataWriters, val next: ActorRef)
    extends RequestAction(dataWriters) with SseAction {

  def sendRequest(requestName: String, session: Session): Validation[Unit] =
    for {
      sseActor <- fetchSse(sseName, session)
    } yield sseActor ! Close(requestName, next, session)
}

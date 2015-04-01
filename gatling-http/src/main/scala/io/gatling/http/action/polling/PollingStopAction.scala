/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.polling

import akka.actor.{ ActorRef, Props }
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.session._
import io.gatling.http.action.UnnamedRequestAction

object PollingStopAction {
  def props(pollerName: String, dataWriters: DataWriters, next: ActorRef): Props =
    Props(new PollingStopAction(pollerName, dataWriters, next))
}
class PollingStopAction(
  pollerName: String,
  dataWriters: DataWriters,
  val next: ActorRef)
    extends UnnamedRequestAction(dataWriters)
    with PollingAction {

  override def sendRequest(requestName: String, session: Session) =
    for {
      pollingActor <- fetchPoller(pollerName, session)
    } yield pollingActor ! StopPolling(next, session)
}

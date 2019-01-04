/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util.Clock
import io.gatling.core.action.{ Action, ActorBasedAction, RequestAction }
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.http.action.sse.fsm.SetCheck
import io.gatling.http.check.sse.SseMessageCheckSequence

class SseSetCheck(
    val requestName: Expression[String],
    checkSequences:  List[SseMessageCheckSequence],
    sseName:         String,
    val statsEngine: StatsEngine,
    val clock:       Clock,
    val next:        Action
) extends RequestAction with ActorBasedAction with SseAction with NameGen {

  override val name = genName("sseSetCheck")

  override def sendRequest(requestName: String, session: Session) =
    for {
      wsActor <- fetchActor(sseName, session)
    } yield {
      // [fl]
      //
      // [fl]
      wsActor ! SetCheck(requestName, checkSequences, session, next)
    }
}

/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
import io.gatling.commons.validation.Validation
import io.gatling.core.action.{ Action, RequestAction }
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

class SseSetCheck(
    val requestName: Expression[String],
    checkSequences: List[SseMessageCheckSequenceBuilder],
    sseName: Expression[String],
    val statsEngine: StatsEngine,
    val clock: Clock,
    val next: Action
) extends RequestAction
    with SseAction
    with NameGen {

  override val name: String = genName("sseSetCheck")

  override def sendRequest(requestName: String, session: Session): Validation[Unit] =
    for {
      fsmName <- sseName(session)
      fsm <- fetchFsm(fsmName, session)
      resolvedCheckSequences <- SseMessageCheckSequenceBuilder.resolve(checkSequences, session)
    } yield {
      // [fl]
      //
      // [fl]
      fsm.onSetCheck(requestName, resolvedCheckSequences, session, next)
    }
}

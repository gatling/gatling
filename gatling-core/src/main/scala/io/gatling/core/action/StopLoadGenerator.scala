/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.core.action

import io.gatling.core.actor.ActorRef
import io.gatling.core.controller.Controller
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

final class StopLoadGenerator(
    message: Expression[String],
    condition: Expression[Boolean],
    crash: Boolean,
    override val statsEngine: StatsEngine,
    controller: ActorRef[Controller.Command],
    override val next: Action
) extends ChainableAction
    with NameGen {
  override val name: String = genName("stopLoadGenerator")

  override def execute(session: Session): Unit = recover(session) {
    for {
      condition <- condition(session)
      msg <- message(session)
    } yield {
      if (condition) {
        controller ! Controller.Command.StopLoadGenerator(msg, crash)
      } else {
        next ! session
      }
    }
  }
}

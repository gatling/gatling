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

package io.gatling.core.action

import io.gatling.commons.stats.KO
import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.session.{ Expression, GroupBlock, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

object ExitHere {
  private[core] val ExitHereOnFailedCondition: Expression[Boolean] = session => (session.status == KO).success
}

class ExitHere(condition: Expression[Boolean], exit: Action, val statsEngine: StatsEngine, val clock: Clock, val next: Action)
    extends ChainableAction
    with NameGen {

  override val name: String = genName("exitHere")

  override def execute(session: Session): Unit = recover(session) {
    condition(session).map { cond =>
      val nextStep =
        if (cond) {
          val now = clock.nowMillis

          session.blockStack.foreach {
            case block: GroupBlock => statsEngine.logGroupEnd(session.scenario, block, now)
            case _                 =>
          }

          exit

        } else {
          next
        }

      nextStep ! session
    }
  }
}

/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

class GroupEnd(statsEngine: StatsEngine, val next: Action) extends ChainableAction with NameGen {

  val name: String = genName("groupEnd")

  def execute(session: Session): Unit =
    session.blockStack match {

      case (group: GroupBlock) :: tail =>
        statsEngine.logGroupEnd(session, group, nowMillis)
        next ! session.exitGroup

      case _ =>
        logger.error(s"GroupEnd called but head of stack ${session.blockStack} isn't a GroupBlock, please report.")
    }
}

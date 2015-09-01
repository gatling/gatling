/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.End
import io.gatling.core.stats.writer.UserMessage

import akka.actor.{ Props, ActorRef }

object Exit {

  val ExitActorName = "gatling-exit"

  def props(controller: ActorRef, statsEngine: StatsEngine) =
    Props(new Exit(controller, statsEngine))
}

class Exit(controller: ActorRef, statsEngine: StatsEngine) extends Action {

  def execute(session: Session): Unit = {
    logger.info(s"End user #${session.userId}")
    session.exit()
    val userEnd = UserMessage(session, End, nowMillis)
    statsEngine.logUser(userEnd)
    controller ! userEnd
  }
}

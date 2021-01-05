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

import java.util.concurrent.TimeUnit

import io.gatling.commons.util.Clock
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine

class Pause(pauseDuration: Expression[Long], val statsEngine: StatsEngine, val clock: Clock, val name: String, val next: Action) extends ExitableAction {

  /**
   * Generates a duration if required or use the one given and defer
   * next actor execution of this duration
   *
   * @param session the session of the virtual user
   */
  override def execute(session: Session): Unit = recover(session) {

    def schedule(durationInMillis: Long): Unit = {
      // can make pause
      logger.debug(s"Pausing for ${durationInMillis}ms")

      session.eventLoop.schedule(
        (() => {
          next ! session
        }): Runnable,
        durationInMillis,
        TimeUnit.MILLISECONDS
      )
    }

    pauseDuration(session).map(schedule)
  }
}

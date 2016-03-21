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

import scala.concurrent.duration.DurationLong

import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine

import akka.actor.ActorSystem

/**
 * PauseAction provides a convenient means to implement pause actions based on random distributions.
 *
 * @param pauseDuration a function that can be used to generate a delay for the pause action
 * @param system the ActorSystem
 * @param statsEngine the StatsEngine
 * @param next the next action to execute, which will be notified after the pause is complete
 */
class Pause(pauseDuration: Expression[Long], system: ActorSystem, val statsEngine: StatsEngine, val name: String, val next: Action) extends ExitableAction {

  import system._

  /**
   * Generates a duration if required or use the one given and defer
   * next actor execution of this duration
   *
   * @param session the session of the virtual user
   */
  override def execute(session: Session): Unit = recover(session) {

      def schedule(durationInMillis: Long) = {
        val drift = session.drift

        if (durationInMillis > drift) {
          // can make pause
          val durationMinusDrift = durationInMillis - drift
          logger.info(s"Pausing for ${durationInMillis}ms (real=${durationMinusDrift}ms)")

          val pauseStart = nowMillis

          try {
            scheduler.scheduleOnce(durationMinusDrift milliseconds) {
              val newDrift = nowMillis - pauseStart - durationMinusDrift
              next ! session.setDrift(newDrift)
            }
          } catch {
            case ise: IllegalStateException => // engine was shutdown
          }

        } else {
          // drift is too big
          val remainingDrift = drift - durationInMillis
          logger.info(s"can't pause (remaining drift=${remainingDrift}ms)")
          next ! session.setDrift(remainingDrift)
        }
      }

    pauseDuration(session).map(schedule)
  }
}

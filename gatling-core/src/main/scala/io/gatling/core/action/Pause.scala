/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.action

import scala.concurrent.duration.DurationLong

import akka.actor.ActorRef
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.TimeHelper.nowMillis

/**
 * PauseAction provides a convenient means to implement pause actions based on random distributions.
 *
 * @param pauseDuration a function that can be used to generate a delay for the pause action
 * @param next the next action to execute, which will be notified after the pause is complete
 */
class Pause(pauseDuration: Expression[Long], val next: ActorRef) extends Interruptable with Failable {

  /**
   * Generates a duration if required or use the one given and defer
   * next actor execution of this duration
   *
   * @param session the session of the virtual user
   */
  def executeOrFail(session: Session) = {

      def schedule(durationInMillis: Long) = {
        val drift = session.drift

        if (durationInMillis > drift) {
          // can make pause
          val durationMinusDrift = durationInMillis - drift
          logger.info(s"Pausing for ${durationInMillis}ms (real=${durationMinusDrift}ms)")

          val pauseStart = nowMillis
          scheduler.scheduleOnce(durationMinusDrift milliseconds) {
            val newDrift = nowMillis - pauseStart - durationMinusDrift
            next ! session.setDrift(newDrift)
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

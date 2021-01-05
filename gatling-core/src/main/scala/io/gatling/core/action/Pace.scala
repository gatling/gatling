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

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.util.Clock
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

/**
 * Pace provides a means to limit the frequency with which an action is run, by specifying a minimum wait time between iterations.
 *
 * Originally contributed by James Pickering.
 *
 * @param intervalExpr a function that decides how long to wait before the next iteration
 * @param counter the name of the counter used to keep track of the run state. Typically this would be random, but
 *                can be set explicitly if needed
 */
class Pace(intervalExpr: Expression[FiniteDuration], counter: String, val statsEngine: StatsEngine, val clock: Clock, val next: Action)
    extends ExitableAction
    with NameGen {

  override val name: String = genName("pace")

  /**
   * Pace keeps track of when it can next run using a counter in the session.
   * If this counter does not exist, it will run immediately.
   * On each run, it increments the counter by intervalExpr.
   *
   * @param session the session of the virtual user
   * @return nothing
   */
  override def execute(session: Session): Unit = recover(session) {
    intervalExpr(session).map { interval =>
      val now = clock.nowMillis
      val intervalMillis = interval.toMillis
      session(counter).asOption[Long] match {
        case Some(timeLimit) if timeLimit > now =>
          session.eventLoop.schedule(
            (() => {
              // clock.nowMillis will be evaluated when scheduled task will run
              next ! session.set(counter, clock.nowMillis + intervalMillis)
            }): Runnable,
            timeLimit - now,
            TimeUnit.MILLISECONDS
          )

        case _ =>
          next ! session.set(counter, now + intervalMillis)
      }
    }
  }
}

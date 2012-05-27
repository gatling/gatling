package com.excilys.ebi.gatling.core.action

/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.concurrent.TimeUnit

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.NumberHelper.getRandomLongFromExp

import akka.actor.ActorRef
import akka.util.duration.longToDurationLong
import grizzled.slf4j.Logging

/**
 * An action for "pausing" a user with a "think time" coming from an exponential distribution with the specified
 * average duration.
 *
 * @constructor creates a PauseAction
 * @param next action that will be executed after the pause duration
 * @param averageDuration average duration of the pause
 * @param timeUnit time unit of the duration
 */
class ExpPauseAction(next: ActorRef, averageDuration: Long, timeUnit: TimeUnit) extends Action with Logging {

  val averageDurationInMillis = TimeUnit.MILLISECONDS.convert(averageDuration, timeUnit)

  /**
   * Generates a pause using an exponential distribution.
   * next actor execution of this duration
   *
   * @param session the session of the virtual user
   */
  def execute(session: Session) {

    val delayInMs = getRandomLongFromExp(averageDurationInMillis)

    val delayAdjustedForLastActionInMs = delayInMs - session.getTimeShift

    info(new StringBuilder().append("Waiting for ").append(delayInMs).append("ms (")
      .append(delayAdjustedForLastActionInMs).append("ms)"))

    system.scheduler.scheduleOnce(delayAdjustedForLastActionInMs milliseconds, next, session)
  }
}

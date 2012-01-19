/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.action

import java.util.concurrent.TimeUnit

import scala.util.Random

import com.excilys.ebi.gatling.core.session.Session

import akka.actor.{Scheduler, ActorRef}

/**
 * PauseAction class companion
 */
object PauseAction {

	/**
	 * used to generate random pause durations
	 */
	val randomGenerator = new Random
}

/**
 * This action represents a pause in the scenario (ie: think time)
 *
 * @constructor creates a PauseAction
 * @param next action that will be executed after the pause duration
 * @param minDuration minimum duration of the pause
 * @param maxDuration maximum duration of the pause
 * @param timeUnit time unit of the duration
 */
class PauseAction(next: ActorRef, minDuration: Long, maxDuration: Long, timeUnit: TimeUnit) extends Action {

	/**
	 * Generates a duration if required or use the one given and defer
	 * next actor execution of this duration
	 *
	 * @param session Session of current user
	 * @return Nothing
	 */
	def execute(session: Session) = {

		val diff = maxDuration - minDuration
		val duration = minDuration + (if (diff > 0) PauseAction.randomGenerator.nextInt(diff.toInt) else 0)

		val durationMinusLastActionDurationInMillis: Long = TimeUnit.MILLISECONDS.convert(duration, timeUnit) - session.getLastActionDuration
		if (logger.isInfoEnabled)
			logger.info("Waiting for {}ms ({}ms)", TimeUnit.MILLISECONDS.convert(duration, timeUnit), durationMinusLastActionDurationInMillis)

		Scheduler.scheduleOnce(() => next! session, durationMinusLastActionDurationInMillis, TimeUnit.MILLISECONDS)
	}
}

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

import scala.concurrent.duration.DurationLong

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis

import akka.actor.ActorRef

/**
 * PauseAction provides a convenient means to implement pause actions based on random distributions.
 *
 * @param generateDelayInMillis a function that can be used to generate a delays for the pause action
 * @param next the next action to execute, which will be notified after the pause is complete
 */
class Pause(generateDelayInMillis: () => Long, val next: ActorRef) extends Bypassable {

	/**
	 * Generates a duration if required or use the one given and defer
	 * next actor execution of this duration
	 *
	 * @param session the session of the virtual user
	 */
	def execute(session: Session) {

		import system.dispatcher

		val durationInMillis: Long = generateDelayInMillis()
		val timeShift = session.getTimeShift

		if (durationInMillis > timeShift) {
			// can make pause
			val durationMinusTimeShift = durationInMillis - timeShift
			info(s"Pausing for ${durationInMillis}ms (real=${durationMinusTimeShift}ms)")

			val pauseStart = nowMillis
			system.scheduler.scheduleOnce(durationMinusTimeShift milliseconds) {
				val newTimeShift = nowMillis - pauseStart - durationMinusTimeShift
				next ! session.setTimeShift(newTimeShift)
			}

		} else {
			// time shift is too big
			val remainingTimeShift = timeShift - durationInMillis
			info(s"can't pause (remaining time shift=${remainingTimeShift}ms)")
			next ! session.setTimeShift(remainingTimeShift)
		}
	}
}

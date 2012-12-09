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
package com.excilys.ebi.gatling.core.session.handler

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.session.Session.GATLING_PRIVATE_ATTRIBUTE_PREFIX
import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis

import scalaz._

/**
 * TimerBasedIterationHandler trait 'companion'
 */
object TimerBasedIterationHandler {

	/**
	 * Key prefix for Counters
	 */
	private val TIMER_KEY_PREFIX = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "core.timer."

	def getTimerAttributeName(counterName: String) = TIMER_KEY_PREFIX + counterName

	def getTimer(session: Session, counterName: String): Validation[String, Long] = session.safeGetAs[Long](getTimerAttributeName(counterName))
}

/**
 * This trait is used for mixin-composition
 *
 * It adds timer based iteration behavior to a class
 */
trait TimerBasedIterationHandler extends CounterBasedIterationHandler {

	override def init(session: Session): Session = {

		val timerAttributeName = TimerBasedIterationHandler.getTimerAttributeName(counterName)

		if (session.contains(timerAttributeName))
			super.init(session)
		else
			super.init(session).set(timerAttributeName, nowMillis)
	}

	override def expire(session: Session) = super.expire(session).remove(TimerBasedIterationHandler.getTimerAttributeName(counterName))
}
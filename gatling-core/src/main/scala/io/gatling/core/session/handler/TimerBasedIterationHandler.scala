/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.session.handler

import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.Validation

object TimerBasedIterationHandler {

	/**
	 * Key prefix for Counters
	 */
	private val timerAttributeNamePrefix = SessionPrivateAttributes.privateAttributePrefix + "core.timer."

	def getTimerAttributeName(counterName: String) = timerAttributeNamePrefix + counterName

	def getTimer(session: Session, counterName: String): Validation[Long] = session.getV[Long](getTimerAttributeName(counterName))
}

/**
 * Adds timer based iteration behavior to a class
 */
trait TimerBasedIterationHandler extends CounterBasedIterationHandler {

	override def init(session: Session): Validation[Session] = {

		val timerAttributeName = TimerBasedIterationHandler.getTimerAttributeName(counterName)

		if (session.contains(timerAttributeName))
			super.init(session)
		else
			super.init(session).map(_.set(timerAttributeName, nowMillis))
	}

	override def expire(session: Session): Validation[Session] = super.expire(session).map(_.remove(TimerBasedIterationHandler.getTimerAttributeName(counterName)))
}
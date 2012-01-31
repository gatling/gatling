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

import java.lang.System.currentTimeMillis

import com.excilys.ebi.gatling.core.session.Session

import TimerBasedIterationHandler.TIMER_KEY_PREFIX

/**
 * TimerBasedIterationHandler trait 'companion'
 */
object TimerBasedIterationHandler {
	/**
	 * Key prefix for Counters
	 */
	val TIMER_KEY_PREFIX = "gatling.core.timer."
		
	def getTimerName(counterName: String) = TIMER_KEY_PREFIX + counterName

	/**
	 * This method gets the specified timer from the session
	 *
	 * @param session the scenario session
	 * @param timerName the name of the timer
	 * @return the value of the timer as a long
	 */
	def getTimerValue(session: Session, timerName: String) = session.getAttributeAsOption[Long](getTimerName(timerName)).getOrElse(throw new IllegalAccessError("Timer is not set : " + timerName))
}

/**
 * This trait is used for mixin-composition
 *
 * It adds timer based iteration behavior to a class
 */
trait TimerBasedIterationHandler extends IterationHandler {

	override def init(session: Session, counterName: String) : Session = {
		
		val newSession = super.init(session, counterName)
		val timerName = TimerBasedIterationHandler.getTimerName(counterName)
		
		if (newSession.getAttributeAsOption(timerName).isDefined) {
			newSession
		} else {
			newSession.setAttribute(timerName, currentTimeMillis)
		}
	}

	override def expire(session: Session, counterName: String) = super.expire(session, counterName).removeAttribute(TimerBasedIterationHandler.getTimerName(counterName))
}
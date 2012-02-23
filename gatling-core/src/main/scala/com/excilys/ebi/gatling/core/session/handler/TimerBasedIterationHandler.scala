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

	private def getTimerAttributeName(timerName: String) = TIMER_KEY_PREFIX + timerName

	/**
	 * This method gets the specified timer from the session
	 *
	 * @param session the scenario session
	 * @param timerName the name of the timer
	 * @return the value of the timer as a long
	 */
	def getTimerValue(session: Session, timerName: String) = session.getAttributeAsOption[Long](getTimerAttributeName(timerName)).getOrElse(throw new IllegalAccessError("Timer is not set : " + timerName))
}

/**
 * This trait is used for mixin-composition
 *
 * It adds timer based iteration behavior to a class
 */
trait TimerBasedIterationHandler extends IterationHandler {
	
	lazy val timerAttributeName = TimerBasedIterationHandler.getTimerAttributeName(counterName)

	override def init(session: Session): Session = {

		session.getAttributeAsOption[Int](timerAttributeName) match {
			case None => super.init(session).setAttribute(timerAttributeName, currentTimeMillis)
			case Some(_) => super.init(session)
		}
	}

	override def expire(session: Session) = super.expire(session).removeAttribute(TimerBasedIterationHandler.getTimerAttributeName(counterName))
}
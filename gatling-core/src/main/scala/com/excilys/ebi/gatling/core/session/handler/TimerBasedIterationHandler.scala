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

import TimerBasedIterationHandler.TIMER_KEY_PREFIX

/**
 * TimerBasedIterationHandler trait 'companion'
 */
object TimerBasedIterationHandler {
	/**
	 * Key prefix for Counters
	 */
	val TIMER_KEY_PREFIX = "gatling.core.timer."

	/**
	 * This method gets the specified timer from the session
	 *
	 * @param session the scenario session
	 * @param timerName the name of the timer
	 * @return the value of the timer as a long
	 */
	def getTimerValue(session: Session, timerName: String) = {
		session.getAttributeAsOption[Long](TIMER_KEY_PREFIX + timerName).getOrElse(throw new IllegalAccessError("You must call startTimer before this method is called"))
	}
}

/**
 * This trait is used for mixin-composition
 *
 * It adds timer based iteration behavior to a class
 */
trait TimerBasedIterationHandler extends IterationHandler {

	abstract override def init(session: Session, uuid: String, userDefinedName: Option[String]) = {
		super.init(session, uuid, userDefinedName)
		session.getAttributeAsOption(TIMER_KEY_PREFIX + uuid).getOrElse {
			session.setAttribute(TIMER_KEY_PREFIX + uuid, System.currentTimeMillis)
		}
	}

	abstract override def expire(session: Session, uuid: String, userDefinedName: Option[String]) = {
		super.expire(session, uuid, userDefinedName)
		session.removeAttribute(TIMER_KEY_PREFIX + uuid)
	}

}
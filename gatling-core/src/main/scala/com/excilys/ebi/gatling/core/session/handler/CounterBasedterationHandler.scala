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

import CounterBasedIterationHandler.COUNTER_KEY_PREFIX

/**
 * CounterBasedIterationHandler trait 'companion'
 */
object CounterBasedIterationHandler {

	/**
	 * Key prefix for Counters
	 */
	val COUNTER_KEY_PREFIX = "gatling.core.counter."

	/**
	 * This method gets the specified counter from the session
	 *
	 * @param session the scenario session
	 * @param counterName the name of the counter
	 * @return the value of the counter as an integer
	 */
	def getCounterValue(session: Session, counterName: String) = {
		session.getAttributeAsOption[Int](COUNTER_KEY_PREFIX + counterName).getOrElse(throw new IllegalAccessError("Counter does not exist, check the name of the key " + counterName))
	}
}

/**
 * This trait is used for mixin-composition
 *
 * It adds counter based iteration behavior to a class
 */
trait CounterBasedIterationHandler extends IterationHandler {

	abstract override def init(session: Session, userDefinedName: Option[String]) = {
		val newSession = super.init(session, userDefinedName)
		val counterName = userDefinedName.getOrElse(uuidAsString)
		
		if (newSession.getAttributeAsOption(COUNTER_KEY_PREFIX + counterName).isDefined) {
			newSession
		} else {
			newSession.setAttribute(COUNTER_KEY_PREFIX + counterName, -1)
		}
	}

	abstract override def increment(session: Session, userDefinedName: Option[String]) = {
		val newSession = super.increment(session, userDefinedName)
		val key = COUNTER_KEY_PREFIX + userDefinedName.getOrElse(uuidAsString)
		val currentValue: Int = newSession.getAttributeAsOption[Int](key).getOrElse(throw new IllegalAccessError("You must call startCounter before this method is called"))

		newSession.setAttribute(key, currentValue + 1)
	}

	abstract override def expire(session: Session, userDefinedName: Option[String]) = {
		super.expire(session, userDefinedName).removeAttribute(COUNTER_KEY_PREFIX + userDefinedName.getOrElse(uuidAsString))
	}
}
/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

	abstract override def init(session: Session, uuid: String, userDefinedName: Option[String]) = {
		super.init(session, uuid, userDefinedName)
		val counterName = userDefinedName.getOrElse(uuid)
		session.getAttributeAsOption(COUNTER_KEY_PREFIX + counterName).getOrElse {
			session.setAttribute(COUNTER_KEY_PREFIX + counterName, -1)
		}
	}

	abstract override def increment(session: Session, uuid: String, userDefinedName: Option[String]) = {
		super.increment(session, uuid, userDefinedName)
		val key = COUNTER_KEY_PREFIX + userDefinedName.getOrElse(uuid)
		val currentValue: Int = session.getAttributeAsOption[Int](key).getOrElse(throw new IllegalAccessError("You must call startCounter before this method is called"))

		session.setAttribute(key, currentValue + 1)
	}

	abstract override def expire(session: Session, uuid: String, userDefinedName: Option[String]) = {
		super.expire(session, uuid, userDefinedName)
		session.removeAttribute(COUNTER_KEY_PREFIX + userDefinedName.getOrElse(uuid))
	}
}
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

/**
 * CounterBasedIterationHandler trait 'companion'
 */
object CounterBasedIterationHandler {

	/**
	 * Key prefix for Counters
	 */
	val COUNTER_KEY_PREFIX = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "core.counter."

	def getCounterAttributeName(counterName: String) = COUNTER_KEY_PREFIX + counterName
}

/**
 * This trait is used for mixin-composition
 *
 * It adds counter based iteration behavior to a class
 */
trait CounterBasedIterationHandler extends IterationHandler {

	lazy val counterAttributeName = CounterBasedIterationHandler.getCounterAttributeName(counterName)

	override def init(session: Session) = 
		if (session.isAttributeDefined(counterAttributeName))
			super.init(session)
		else
			super.init(session).setAttribute(counterAttributeName, -1)

	override def increment(session: Session) = session.getAttributeAsOption[Int](counterAttributeName)
		.map {
			currentValue => super.increment(session).setAttribute(counterAttributeName, currentValue + 1)
		}.getOrElse {
			throw new IllegalAccessError("You must call startCounter before this method is called")
		}

	override def expire(session: Session) = super.expire(session).removeAttribute(CounterBasedIterationHandler.getCounterAttributeName(counterName))
}
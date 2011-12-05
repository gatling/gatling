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
package com.excilys.ebi.gatling.core.action.builder
import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder.simpleActionBuilder
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.session.handler.CounterBasedIterationHandler
import com.excilys.ebi.gatling.core.session.Session

import IterationStep.{ IterationStep, INIT, INCREMENT, EXPIRE }

/**
 * This enumeration lists the different steps in an iteration regarding the counter :
 *   INIT:      the initialization
 *   INCREMENT: the incrementation
 *   EXPIRE:    the release
 */
object IterationStep extends Enumeration {
	type IterationStep = Value
	val INIT, INCREMENT, EXPIRE = Value
}

/**
 * This builder is used to create simple actions containing the functions that
 * will be used to create a times loop.
 */
object CountBasedIterationActionBuilder extends CounterBasedIterationHandler {
	/**
	 * Creates a builder for a simple action that initializes the counter
	 */
	def initCounterAction(counterName: String) = initClass(counterName, INIT)
	/**
	 * Creates a builder for a simple action that increments the counter
	 */
	def incrementCounterAction(counterName: String) = initClass(counterName, INCREMENT)
	/**
	 * Creates a builder for a simple action that releases the counter
	 */
	def expireCounterAction(counterName: String) = initClass(counterName, EXPIRE)

	/**
	 * Function that actually creates the simple action builder required
	 */
	private def initClass(counterName: String, iterationStep: IterationStep) = {
		val sessionFunction = iterationStep match {
			case INIT => (s: Session, a: Action) => init(s, a.getUuidAsString, Some(counterName))
			case INCREMENT => (s: Session, a: Action) => increment(s, a.getUuidAsString, Some(counterName))
			case EXPIRE => (s: Session, a: Action) => expire(s, a.getUuidAsString, Some(counterName))
		}
		simpleActionBuilder(sessionFunction)
	}
}

/*
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
package com.excilys.ebi.gatling.core.action
import com.excilys.ebi.gatling.core.context.Context
import java.util.concurrent.atomic.AtomicBoolean
import com.excilys.ebi.gatling.core.context.handler.CounterBasedIterationHandler
import com.excilys.ebi.gatling.core.context.handler.TimerBasedIterationHandler

/**
 * Represents a While in the scenario.
 *
 * @constructor creates a While loop in the scenario
 * @param testFunction the function that will be used to decide when to stop the loop
 * @param loopNext the chain executed if testFunction evaluates to true, passed as a Function for construct time
 * @param next the chain executed if testFunction evaluates to false
 * @param counterName the name of the counter for this loop
 */
class WhileAction(testFunction: (Context, Action) => Boolean, var loopNext: WhileAction => Action, next: Action, counterName: Option[String])
		extends Action with TimerBasedIterationHandler with CounterBasedIterationHandler {

	val loopNextAction = loopNext(this)

	/**
	 * Evaluates the testFunction and if true executes the first action of loopNext
	 * else it executes the first action of next
	 *
	 * @param context Context of the scenario
	 * @return Nothing
	 */
	def execute(context: Context) = {
		val uuid = getContext.uuid.toString

		init(context, uuid, counterName)

		increment(context, uuid, counterName)

		if (testFunction(context, this)) {
			loopNextAction.execute(context)
		} else {
			expire(context, uuid, counterName)
			next.execute(context)
		}
	}
}
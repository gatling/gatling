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
package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.session.handler.{ CounterBasedIterationHandler, TimerBasedIterationHandler }

import akka.actor.ActorRef

/**
 * Action in charge of controlling a while loop execution.
 *
 * @constructor creates a While loop in the scenario
 * @param condition the condition that decides when to exit the loop
 * @param next the chain executed if testFunction evaluates to false
 * @param counterName the name of the counter for this loop
 */
class WhileAction(condition: Session => Boolean, val next: ActorRef, val counterName: String) extends Action with TimerBasedIterationHandler with Bypass {

	var loopNextAction: ActorRef = _

	def uninitialized: Receive = {
		case actor: ActorRef =>
			loopNextAction = actor
			context.become(initialized)
	}

	def initialized: Receive = super.receive

	override def receive = uninitialized

	/**
	 * Evaluates the condition and if true executes the first action of loopNext
	 * else it executes next
	 *
	 * @param session the session of the virtual user
	 */
	def execute(session: Session) {

		val sessionWithTimerIncremented = increment(init(session))

		val evaluatedCondition = try {
			condition(sessionWithTimerIncremented)
		} catch {
			case e: Exception =>
				warn("Condition evaluation crashed, exiting loop", e)
				false
		}

		if (evaluatedCondition)
			loopNextAction ! sessionWithTimerIncremented
		else
			next ! expire(session)
	}
}
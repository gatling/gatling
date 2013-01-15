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

import com.excilys.ebi.gatling.core.session.{ Expression, Session }
import com.excilys.ebi.gatling.core.session.handler.{ CounterBasedIterationHandler, TimerBasedIterationHandler }

import akka.actor.ActorRef
import scalaz.{ Failure, Success }
import scalaz.Scalaz.ToValidationV

/**
 * Action in charge of controlling a while loop execution.
 *
 * @constructor creates a While loop in the scenario
 * @param condition the condition that decides when to exit the loop
 * @param counterName the name of the counter for this loop
 * @param next the chain executed if testFunction evaluates to false
 */
class While(condition: Expression[Boolean], val counterName: String, val next: ActorRef) extends Bypassable with TimerBasedIterationHandler with CounterBasedIterationHandler {

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

		// as WhileAction is not supervised, there's no one to restore its state (loopNextAction) on crash, so we try to avoid it
		val evaluatedCondition =
			try condition(sessionWithTimerIncremented)
			catch {
				case e: Exception =>
					error("Loop condition evaluation crashed", e)
					("Loop condition evaluation crashed: " + e.getMessage).failure
			}

		evaluatedCondition match {
			case Success(true) => loopNextAction ! sessionWithTimerIncremented
			case Success(false) => next ! expire(session)
			case Failure(message) =>
				error(s"Error, exiting loop $message")
				next ! expire(session)
		}
	}
}
/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
import com.excilys.ebi.gatling.core.session.handler.TimerBasedIterationHandler
import com.excilys.ebi.gatling.core.validation.Success

import akka.actor.{ Actor, ActorRef, Props }

/**
 * Action in charge of controlling a while loop execution.
 *
 * @constructor creates a While loop in the scenario
 * @param condition the condition that decides when to exit the loop
 * @param counterName the name of the counter for this loop
 * @param next the chain executed if testFunction evaluates to false
 */
class While(condition: Expression[Boolean], counterName: String, next: ActorRef) extends Actor {

	var innerWhile: ActorRef = _

	def uninitialized: Receive = {
		case loopNextAction: ActorRef =>
			innerWhile = context.actorOf(Props(new InnerWhile(condition, loopNextAction, counterName, next)))
			context.become(initialized)
	}

	def initialized: Receive = { case m => innerWhile forward m }

	override def receive = uninitialized
}

class InnerWhile(condition: Expression[Boolean], loopNextAction: ActorRef, val counterName: String, val next: ActorRef) extends Bypassable with TimerBasedIterationHandler {

	/**
	 * Evaluates the condition and if true executes the first action of loopNext
	 * else it executes next
	 *
	 * @param session the session of the virtual user
	 */
	def execute(session: Session) {

		val sessionWithTimerIncremented = increment(init(session))

		condition(sessionWithTimerIncremented) match {
			case Success(true) => loopNextAction ! sessionWithTimerIncremented
			case _ => next ! expire(session)
		}
	}
}
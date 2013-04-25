/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.action

import akka.actor.{ Actor, ActorRef, Props }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.session.handler.TimerBasedIterationHandler
import io.gatling.core.validation.{ Failure, Success }

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

	val uninitialized: Receive = {
		case loopNextAction: ActorRef =>
			innerWhile = context.actorOf(Props(new InnerWhile(condition, loopNextAction, counterName, next)))
			context.become(initialized)
	}

	val initialized: Receive = { case m => innerWhile forward m }

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

		val exit = () => expire(session) match {
			case Success(s) => next ! s
			case Failure(message) =>
				logger.error(s"Could not expire loop: $message")
				next ! session
		}

		val doNext = for {
			initialized <- init(session)
			incremented <- increment(initialized)
			cond <- condition(incremented)
		} yield if (cond) () => loopNextAction ! incremented else exit

		doNext match {
			case Success(f) => f()
			case Failure(message) =>
				logger.error(s"While condition evaluation failed: $message, exiting loop")
				exit()
		}
	}
}
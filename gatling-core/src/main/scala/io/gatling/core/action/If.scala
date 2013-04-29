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

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ Failure, Success }

import akka.actor.ActorRef

/**
 * A conditional Action
 *
 * @constructor create an IfAction
 * @param condition the condition that decides whether to execute thenNext or elseNext
 * @param thenNext the chain of actions executed if condition evaluates to true
 * @param elseNext chain of actions executed if condition evaluates to false
 * @param next chain of actions executed if condition evaluates to false and elseNext equals None
 */
class If(condition: Expression[Boolean], thenNext: ActorRef, elseNext: Option[ActorRef], val next: ActorRef) extends Interruptable {

	/**
	 * Evaluates the condition and decides what to do next
	 *
	 * @param session the session of the virtual user
	 */
	def execute(session: Session) {

		val nextAction = condition(session) match {
			case Success(true) => thenNext
			case Success(false) => next
			case Failure(message) => logger.error(s"Could not resolve loop condition: $message"); next
		}
		nextAction ! session
	}
}
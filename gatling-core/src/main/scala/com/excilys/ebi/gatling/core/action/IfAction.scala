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
import scala.annotation.implicitNotFound

import com.excilys.ebi.gatling.core.session.Session

import akka.actor.ActorRef

/**
 * This class represents a conditional Action
 *
 * @constructor creates an IfAction
 * @param conditionFunction this function is the condition that decides of what action to execute next
 * @param thenNext chain of actions executed if conditionFunction evaluates to true
 * @param elseNext chain of actions executed if conditionFunction evaluates to false
 * @param next chain of actions executed if conditionFunction evaluates to false and elseNext equals None
 */
class IfAction(conditionFunction: Session => Boolean, thenNext: ActorRef, elseNext: Option[ActorRef], next: ActorRef) extends Action {

	/**
	 * Evaluates the conditionFunction and if true executes the first action of thenNext
	 * else it executes the first action of elseNext.
	 *
	 * If there is no elseNext, then, next is executed
	 *
	 * @param session Session for current user
	 * @return Nothing
	 */
	def execute(session: Session) = if (conditionFunction(session)) thenNext !session else elseNext.getOrElse(next) ! session
}
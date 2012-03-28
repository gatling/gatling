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
package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.IfAction
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.structure.ChainBuilder

import akka.actor.Actor.actorOf
import akka.actor.ActorRef

object IfActionBuilder {

	/**
	 * Creates an initialized IfActionBuilder
	 */
	def ifActionBuilder = new IfActionBuilder(null, null, null, null)
}

/**
 * Builder for IfAction
 *
 * @constructor create a new IfActionBuilder
 * @param condition condition of the if
 * @param thenNext chain that will be executed if condition evaluates to true
 * @param elseNext chain that will be executed if condition evaluates to false
 * @param next chain that will be executed if condition evaluates to false and there is no elseNext
 */
class IfActionBuilder(condition: Session => Boolean, thenNext: ChainBuilder, elseNext: Option[ChainBuilder], next: ActorRef) extends ActionBuilder {

	/**
	 * Adds condition to builder
	 *
	 * @param condition the condition function
	 * @return a new builder with condition set
	 */
	def withCondition(condition: Session => Boolean) = new IfActionBuilder(condition, thenNext, elseNext, next)

	/**
	 * Adds thenNext to builder
	 *
	 * @param thenNext the chain executed if condition evaluated to true
	 * @return a new builder with thenNext set
	 */
	def withThenNext(thenNext: ChainBuilder) = new IfActionBuilder(condition, thenNext, elseNext, next)

	/**
	 * Adds elseNext to builder
	 *
	 * @param elseNext the chain executed if condition evaluated to false
	 * @return a new builder with elseNext set
	 */
	def withElseNext(elseNext: Option[ChainBuilder]) = new IfActionBuilder(condition, thenNext, elseNext, next)

	def withNext(next: ActorRef) = new IfActionBuilder(condition, thenNext, elseNext, next)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {
		val actionTrue = thenNext.withNext(next).build(protocolConfigurationRegistry)
		val actionFalse = elseNext.map(_.withNext(next).build(protocolConfigurationRegistry))

		actorOf(new IfAction(condition, actionTrue, actionFalse, next)).start
	}
}
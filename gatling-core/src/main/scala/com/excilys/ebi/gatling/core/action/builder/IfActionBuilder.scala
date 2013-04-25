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
package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.{ system, IfAction }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.structure.ChainBuilder

import akka.actor.{ Props, ActorRef }

object IfActionBuilder {

	/**
	 * Creates an initialized IfActionBuilder
	 */
	def apply(condition: Session => Boolean, thenNext: ChainBuilder, elseNext: Option[ChainBuilder]) = new IfActionBuilder(condition, thenNext, elseNext, null)
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

	def withNext(next: ActorRef) = new IfActionBuilder(condition, thenNext, elseNext, next)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {
		val actionTrue = thenNext.withNext(next).build(protocolConfigurationRegistry)
		val actionFalse = elseNext.map(_.withNext(next).build(protocolConfigurationRegistry))

		system.actorOf(Props(new IfAction(condition, actionTrue, actionFalse, next)))
	}
}
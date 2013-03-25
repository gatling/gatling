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
package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.{ If, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.session.Expression
import com.excilys.ebi.gatling.core.structure.ChainBuilder

import akka.actor.{ ActorRef, Props }

/**
 * @constructor create a new IfBuilder
 * @param condition condition of the if
 * @param thenNext chain that will be executed if condition evaluates to true
 * @param elseNext chain that will be executed if condition evaluates to false
 */
class IfBuilder(condition: Expression[Boolean], thenNext: ChainBuilder, elseNext: Option[ChainBuilder]) extends ActionBuilder {

	def build(next: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {
		val actionTrue = thenNext.build(next, protocolConfigurationRegistry)
		val actionFalse = elseNext.map(_.build(next, protocolConfigurationRegistry))

		system.actorOf(Props(new If(condition, actionTrue, actionFalse, next)))
	}
}
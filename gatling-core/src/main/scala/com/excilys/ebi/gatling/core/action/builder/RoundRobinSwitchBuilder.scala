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

import com.excilys.ebi.gatling.core.action.{ SwitchAction, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.structure.ChainBuilder
import com.excilys.ebi.gatling.core.util.RoundRobin

import akka.actor.{ ActorRef, Props }

object RoundRobinSwitchBuilder {

	def roundRobinSwitchBuilder = new RoundRobinSwitchBuilder(null, null)
}

class RoundRobinSwitchBuilder(possibilities: List[ChainBuilder], next: ActorRef) extends ActionBuilder {

	def withPossibilities(possibilities: List[ChainBuilder]) = new RoundRobinSwitchBuilder(possibilities, next)

	def withNext(next: ActorRef) = new RoundRobinSwitchBuilder(possibilities, next)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {

		if (possibilities.size < 2) throw new IllegalArgumentException("Can't build a round robin switch with less than 2 possibilities")

		val possibleActions = possibilities.map(_.withNext(next).build(protocolConfigurationRegistry))

		val rr = new RoundRobin(possibleActions)

		val strategy = () => rr.next

		system.actorOf(Props(new SwitchAction(strategy, next)))
	}
}
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

import com.excilys.ebi.gatling.core.action.{ Switch, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.structure.ChainBuilder
import com.excilys.ebi.gatling.core.util.RoundRobin

import akka.actor.{ ActorRef, Props }

class RoundRobinSwitchBuilder(possibilities: List[ChainBuilder]) extends ActionBuilder {

	require(possibilities.size >= 2, "Can't build a round robin switch with less than 2 possibilities")

	def build(next: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {

		val possibleActions = possibilities.map(_.build(next, protocolConfigurationRegistry))

		val rr = RoundRobin(possibleActions)

		val strategy = () => rr.next

		system.actorOf(Props(new Switch(strategy, next)))
	}
}
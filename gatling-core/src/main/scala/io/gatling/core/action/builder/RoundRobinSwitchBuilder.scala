/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.action.builder

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import io.gatling.core.action.Switch
import io.gatling.core.config.Protocols
import io.gatling.core.session.Expression
import io.gatling.core.structure.ChainBuilder
import io.gatling.core.util.RoundRobin
import io.gatling.core.validation.SuccessWrapper

class RoundRobinSwitchBuilder(possibilities: List[ChainBuilder]) extends ActionBuilder {

	require(possibilities.size >= 2, "Round robin switch requires at least 2 possibilities")

	def build(next: ActorRef, protocols: Protocols) = {

		val possibleActions = possibilities.map(_.build(next, protocols)).toArray
		val roundRobin = RoundRobin(possibleActions)

		val nextAction: Expression[ActorRef] = _ => roundRobin.next.success

		actor(new Switch(nextAction, next))
	}

	override def registerDefaultProtocols(protocols: Protocols) = {

		val actionBuilders = possibilities.flatMap(_.actionBuilders)

		actionBuilders.foldLeft(protocols) { (protocols, actionBuilder) =>
			actionBuilder.registerDefaultProtocols(protocols)
		}
	}
}

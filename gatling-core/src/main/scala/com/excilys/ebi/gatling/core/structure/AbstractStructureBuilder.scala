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
package com.excilys.ebi.gatling.core.structure

import com.excilys.ebi.gatling.core.action.builder.ActionBuilder
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.structure.loop.LoopBuilder

import akka.actor.ActorRef
import grizzled.slf4j.Logging

/**
 * This class defines most of the scenario related DSL
 *
 * @param actionBuilders the builders that represent the chain of actions of a scenario/chain
 */
abstract class AbstractStructureBuilder[B <: AbstractStructureBuilder[B]] extends Execs[B] with Pauses[B] with Feeds[B] with Loops[B] with ConditionalStatements[B] with Errors[B] with Logging {

	/**
	 * Method used to declare a loop
	 *
	 * @param chain the chain of actions that should be repeated
	 */
	@deprecated("Will be removed in Gatling 1.4.0.", "1.3.0")
	def loop(chain: ChainBuilder) = new LoopBuilder[B](getInstance, chain, None)

	@deprecated("Will be removed in Gatling 1.4.0.", "1.3.0")
	private[core] def addActionBuilders(actionBuildersToAdd: List[ActionBuilder]): B = newInstance(actionBuildersToAdd ::: actionBuilders)

	protected def buildChainedActions(entryPoint: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry): ActorRef = actionBuilders
		.foldLeft(entryPoint) { (actorRef, actionBuilder) =>
			actionBuilder.withNext(actorRef).build(protocolConfigurationRegistry)
		}
}


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

import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry

import akka.actor.ActorRef
import grizzled.slf4j.Logging

/**
 * This class defines most of the scenario related DSL
 *
 * @param actionBuilders the builders that represent the chain of actions of a scenario/chain
 */
abstract class AbstractStructureBuilder[B <: AbstractStructureBuilder[B]] extends Execs[B] with Pauses[B] with Feeds[B] with Loops[B] with ConditionalStatements[B] with Errors[B] with Groups[B] with Logging {

	protected def buildChainedActions(entryPoint: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry): ActorRef = actionBuilders
		.foldLeft(entryPoint) { (actorRef, actionBuilder) =>
			actionBuilder.build(actorRef, protocolConfigurationRegistry)
		}
}


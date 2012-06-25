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

import java.util.UUID.randomUUID

import com.excilys.ebi.gatling.core.action.{ system, WhileAction }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.structure.ChainBuilder

import akka.actor.{ Props, ActorRef }

object WhileActionBuilder {
	/**
	 * Creates an initialized WhileActionBuilder
	 */
	def whileActionBuilder = new WhileActionBuilder(null, null, null, randomUUID.toString)
}

/**
 * Builder for WhileActionBuilder
 *
 * @constructor create a new WhileAction
 * @param condition the function that determine the condition
 * @param loopNext chain that will be executed if condition evaluates to true
 * @param next action that will be executed if condition evaluates to false
 */
class WhileActionBuilder(condition: Session => Boolean, loopNext: ChainBuilder, next: ActorRef, counterName: String) extends ActionBuilder {

	/**
	 * Adds condition to this builder
	 *
	 * @param condition the condition function
	 * @return a new builder with condition set
	 */
	def withCondition(condition: Session => Boolean): WhileActionBuilder = new WhileActionBuilder(condition, loopNext, next, counterName)

	/**
	 * Adds loopNext to builder
	 *
	 * @param loopNext the chain executed if testFunction evaluated to true
	 * @return a new builder with loopNext set
	 */
	def withLoopNext(loopNext: ChainBuilder) = new WhileActionBuilder(condition, loopNext, next, counterName)

	/**
	 * Adds counterName to builder
	 *
	 * @param counterName the name of the counter that will be used
	 * @return a new builder with counterName set to None or Some(name)
	 */
	def withCounterName(counterName: String) = new WhileActionBuilder(condition, loopNext, next, counterName)

	def withNext(next: ActorRef) = new WhileActionBuilder(condition, loopNext, next, counterName)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = system.actorOf(Props(new WhileAction(condition, (next: ActorRef) => loopNext.withNext(next).build(protocolConfigurationRegistry), next, counterName)))
}
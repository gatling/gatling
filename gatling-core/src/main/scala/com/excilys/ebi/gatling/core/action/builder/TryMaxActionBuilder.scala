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

import com.excilys.ebi.gatling.core.action.{ TryMaxAction, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.structure.ChainBuilder

import akka.actor.{ ActorRef, Props }

object TryMaxActionBuilder {

	/**
	 * Creates an initialized TryMaxActionBuilder
	 */
	def tryMaxActionBuilder = new TryMaxActionBuilder(1, null, null, randomUUID.toString)
}

class TryMaxActionBuilder(times: Int, loopNext: ChainBuilder, next: ActorRef, counterName: String) extends ActionBuilder {

	/**
	 * Set tryMaxs number to this builder
	 *
	 * @param times the tryMaxs max number
	 * @return a new builder with times set
	 */
	def withTimes(times: Int): TryMaxActionBuilder = new TryMaxActionBuilder(times, loopNext, next, counterName)

	/**
	 * Adds loopNext to builder
	 *
	 * @param loopNext the chain executed if testFunction evaluated to true
	 * @return a new builder with loopNext set
	 */
	def withLoopNext(loopNext: ChainBuilder) = new TryMaxActionBuilder(times, loopNext, next, counterName)

	/**
	 * Adds counterName to builder
	 *
	 * @param counterName the name of the counter that will be used
	 * @return a new builder with counterName set to None or Some(name)
	 */
	def withCounterName(counterName: String) = new TryMaxActionBuilder(times, loopNext, next, counterName)

	def withNext(next: ActorRef) = new TryMaxActionBuilder(times, loopNext, next, counterName)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {
		val tryMaxActor = system.actorOf(Props(new TryMaxAction(times, next, counterName)))
		val loopContent = loopNext.withNext(tryMaxActor).build(protocolConfigurationRegistry)
		tryMaxActor ! loopContent
		tryMaxActor
	}
}
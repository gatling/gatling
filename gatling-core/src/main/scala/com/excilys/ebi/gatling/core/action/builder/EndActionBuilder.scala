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

import com.excilys.ebi.gatling.core.action.{ EndAction, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry

import akka.actor.{ ActorRef, Props }

object EndActionBuilder {

	private val empty = new EndActionBuilder

	/**
	 * Creates a new EndActionBuilder
	 *
	 * @return a ready to use EndActionBuilder
	 */
	def apply() = empty
}

/**
 * Builder for EndAction
 *
 * @constructor create an EndActionBuilder
 * @param latch The CountDownLatch that will stop the simulation
 */
class EndActionBuilder extends ActionBuilder {

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = system.actorOf(Props[EndAction])

	def withNext(next: ActorRef): ActionBuilder = this
}

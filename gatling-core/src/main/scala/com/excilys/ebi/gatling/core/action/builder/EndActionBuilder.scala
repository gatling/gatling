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

import java.util.concurrent.CountDownLatch
import com.excilys.ebi.gatling.core.action.{EndAction, Action}
import akka.actor.Actor.actorOf
import akka.actor.ActorRef
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry

/**
 * EndActionBuilder class companion
 */
object EndActionBuilder {

	/**
	 * Creates a new EndActionBuilder
	 *
	 * @param countDownLatch the countdown latch that will stop the simulation execution
	 * @return An EndActionBuilder ready to use
	 */
	def endActionBuilder(latch: CountDownLatch) = new EndActionBuilder(latch)
}

/**
 * Builder for EndAction
 *
 * @constructor create an EndActionBuilder with its countdown slatch
 * @param latch The CountDownLatch that will stop the simulation
 */
class EndActionBuilder(latch: CountDownLatch) extends ActionBuilder {

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = actorOf(new EndAction(latch)).start
		
	def withNext(next: ActorRef): ActionBuilder = this

	override def toString = "End"
}

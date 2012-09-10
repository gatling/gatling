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

import akka.actor.ActorRef
import com.excilys.ebi.gatling.core.action.{ system, PauseAction }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import akka.actor.Props

object CustomPauseActionBuilder {

	def apply(delayGenerator: () => Long) = new CustomActionPauseBuilder(delayGenerator, null)
}

/**
 * Builder for the custom 'pause' action.
 *
 * @constructor create a new PauseActionBuilder
 * @param delayGenerator the strategy for computing the duration of the generated pause, in milliseconds
 * @param next action that will be executed after the generated pause
 */
class CustomActionPauseBuilder(delayGenerator: () => Long, next: ActorRef) extends ActionBuilder {

	def withNext(next: ActorRef) = new CustomActionPauseBuilder(delayGenerator, next)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = system.actorOf(Props(new PauseAction(next, delayGenerator)))
}
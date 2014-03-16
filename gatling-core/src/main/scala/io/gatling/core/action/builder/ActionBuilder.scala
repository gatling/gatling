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

import akka.actor.ActorRef
import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.config.{ Protocol, Protocols }

/**
 * Top level abstraction for components in charge of building Actions.
 * ActionBuilder is what is passed to the DSL exec() method.
 */
trait ActionBuilder extends AkkaDefaults {

	/**
	 * @param next the Action that will be chained with the Action build by this builder
	 * @param protocols the protocols configurations
	 * @return the resulting Action actor
	 */
	def build(next: ActorRef, protocols: Protocols): ActorRef

	/**
	 * Register default values of the protocols that the Actions produced by this ActionBuilder will use.
	 * With this, the simulation is aware of the protocols and can trigger warmups.
	 *
	 * @param protocols the default protocols
	 * @return the defaultprotocols updated with the ones used here
	 */
	def registerDefaultProtocols(protocols: Protocols): Protocols = protocols
}

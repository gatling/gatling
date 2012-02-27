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
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry

/**
 * This trait represents an Action Builder
 */
trait AbstractActionBuilder {

	/**
	 * Adds next action to this builder, to be able to chain the actions
	 *
	 * @param next Action that will be executed after the one built by this builder
	 * @return A builder of the same type, with next set
	 */
	private[gatling] def withNext(next: ActorRef): AbstractActionBuilder

	/**
	 * Builds the Action
	 *
	 * @param protocolConfigurationRegistry
	 * @return The built Action
	 */
	private[gatling] def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry): ActorRef
}
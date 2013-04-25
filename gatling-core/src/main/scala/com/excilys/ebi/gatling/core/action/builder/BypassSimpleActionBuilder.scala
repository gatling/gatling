/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import com.excilys.ebi.gatling.core.action.{ Bypass, SimpleAction, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.session.Session

import akka.actor.{ ActorRef, Props }

object BypassSimpleActionBuilder {

	/**
	 * Creates a simple action builder with bypass
	 *
	 * @param sessionFunction the function that will be executed by the built simple action
	 */
	def apply(sessionFunction: Session => Session) = new BypassSimpleActionBuilder(sessionFunction, null)
}

/**
 * Builder for SimpleAction with bypass
 *
 * @constructor creates a SimpleActionBuilder
 * @param sessionFunction the function that will be executed by the simple action
 * @param next the action that will be executed after the simple action built by this builder
 */
class BypassSimpleActionBuilder(sessionFunction: Session => Session, next: ActorRef) extends ActionBuilder {

	def withNext(next: ActorRef) = new SimpleActionBuilder(sessionFunction, next)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = system.actorOf(Props(new SimpleAction(sessionFunction, next) with Bypass))
}
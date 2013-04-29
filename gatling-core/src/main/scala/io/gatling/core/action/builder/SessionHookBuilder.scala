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
package io.gatling.core.action.builder

import akka.actor.{ ActorRef, Props }
import io.gatling.core.action.{ Interruptable, SessionHook, system }
import io.gatling.core.config.ProtocolConfigurationRegistry
import io.gatling.core.session.{ Expression, Session }

/**
 * Builder for SimpleAction
 *
 * @constructor creates a SimpleActionBuilder
 * @param sessionFunction the function that will be executed by the simple action
 */
class SessionHookBuilder(sessionFunction: Expression[Session], bypassable: Boolean = false) extends ActionBuilder {

	def build(next: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry) =
		if (bypassable)
			system.actorOf(Props(new SessionHook(sessionFunction, next) with Interruptable))
		else
			system.actorOf(Props(new SessionHook(sessionFunction, next)))
}
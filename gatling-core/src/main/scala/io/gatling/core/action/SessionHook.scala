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
package io.gatling.core.action

import akka.actor.ActorRef
import io.gatling.core.session.{ Expression, Session }

/**
 * Hook for interacting with the Session
 *
 * @constructor Constructs a SimpleAction
 * @param sessionFunction a function for manipulating the Session
 * @param next the action to be executed after this one
 */
class SessionHook(sessionFunction: Expression[Session], val next: ActorRef) extends Chainable with Failable {

	/**
	 * Applies the function to the Session
	 *
	 * @param session the session of the virtual user
	 */
	def executeOrFail(session: Session) = sessionFunction(session).map(newSession => next ! newSession)
}

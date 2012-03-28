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
package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.session.Session

import akka.actor.ActorRef

/**
 * Hook for interacting with the Session
 *
 * @constructor Constructs a SimpleAction
 * @param sessionFunction a function for manipulating the Session
 * @param next the action to be executed after this one
 */
class SimpleAction(sessionFunction: Session => Session, next: ActorRef) extends Action {

	/**
	 * Applies the function to the Session
	 *
	 * @param session the session of the virtual user
	 */
	def execute(session: Session) {
		next ! sessionFunction(session)
	}
}
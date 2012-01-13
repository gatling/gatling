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
 * This class represents a simple action. That is to say an action responsible for executing
 * functions that interacts with the session
 *
 * @constructor Constructs a SimpleAction
 * @param sessionModifier the function that will be executed by this action
 * @param next the action to be executed after this one
 */
class SimpleAction(sessionFunction: (Session, Action) => Unit, next: ActorRef) extends Action {

	/**
	 * This method applies the function to the Session
	 *
	 * @param session The session of the scenario
	 */
	def execute(session: Session) = {
		sessionFunction(session, this)
		next ! session
	}
}
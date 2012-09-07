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

import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.session.Session

import akka.actor.ActorRef

object StartAction {

	/**
	 * Name of the StartAction used in simulation.log
	 */
	val START_OF_SCENARIO = "Start of scenario"
}

/**
 * An Action that is automatically prepended at the beginning of a scenario.
 *
 * @constructor create an StartAction
 * @param next the action to be executed after this one
 */
class StartAction(next: ActorRef) extends Action(StartAction.START_OF_SCENARIO, next) {

	/**
	 * Sends a message to the DataWriter and gives hand to next actor
	 *
	 * @param session the session of the virtual user
	 */
	def execute(session: Session) {

		DataWriter.startUser(session.scenarioName, session.userId)
		info("Starting user #" + session.userId)
		next ! session
	}
}
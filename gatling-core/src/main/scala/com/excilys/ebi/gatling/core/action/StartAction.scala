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

import com.excilys.ebi.gatling.core.result.message.ResultStatus.OK
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.session.Session

import akka.actor.Actor.registry.actorFor
import akka.actor.ActorRef

/**
 * StartAction class companion
 */
object StartAction {

	/**
	 * This variable contains the name of the StartAction used in simulation.log
	 */
	val START_OF_SCENARIO = "Start of Scenario"
}

/**
 * StartAction class represents the first action of the scenario, ie: its beginning.
 *
 * @constructor creates an StartAction
 * @param next the action to be executed after this one
 */
class StartAction(next: ActorRef) extends Action {

	/**
	 * Sends a message to the DataWriter and give hand to next actor
	 *
	 * @param session The session of the current user
	 */
	def execute(session: Session) = {
		val now = System.currentTimeMillis
		actorFor(session.writeActorUuid).map(_ ! ActionInfo(session.scenarioName, session.userId, StartAction.START_OF_SCENARIO, now, now, now, now, OK, "Beginning Scenario"))
		logger.info("Starting user #{}", session.userId)
		next !session
	}
}
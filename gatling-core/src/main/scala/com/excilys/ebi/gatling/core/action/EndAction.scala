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

import java.lang.System.currentTimeMillis
import java.util.concurrent.CountDownLatch

import com.excilys.ebi.gatling.core.result.message.ResultStatus.OK
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.session.Session

import akka.actor.Actor.registry.actorFor

/**
 * EndAction class companion
 */
object EndAction {
	/**
	 * This variable contains the name of the EndAction used in simulation.log
	 */
	val END_OF_SCENARIO = "End of scenario"
}
/**
 * EndAction class represents the last action of the scenario.
 *
 * @constructor creates an EndAction
 * @param latch The countdown latch that will end the simulation
 */
class EndAction(val latch: CountDownLatch) extends Action {

	/**
	 * Sends a message to the DataWriter and decreases the countDownLatch
	 *
	 * @param session The session of the current user that finishes
	 */
	def execute(session: Session) = {
		val now = currentTimeMillis
		actorFor(session.writeActorUuid).map(_ ! ActionInfo(session.scenarioName, session.userId, EndAction.END_OF_SCENARIO, now, now, now, now, OK, "End of Scenario Reached"))

		latch.countDown

		logger.info("Done user #{}", session.userId)
	}
}

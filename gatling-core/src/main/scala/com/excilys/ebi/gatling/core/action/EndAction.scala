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

import com.excilys.ebi.gatling.core.result.terminator.Terminator
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.session.Session

import grizzled.slf4j.Logging

object EndAction {

	/**
	 * Name of the EndAction used in simulation.log
	 */
	val END_OF_SCENARIO = "End of scenario"
}

/**
 * An Action that is automatically appended at the end of a scenario.
 * Used for detecting that a user has finished running, so that the engine can be shutdown once all of them are done.
 *
 * @constructor create an EndAction
 * @param latch used to block the main Thread until all users are finished and then shut the engine down
 */
class EndAction extends Action with Logging {

	/**
	 * Sends a message to the DataWriter and decreases the countDownLatch
	 *
	 * @param session the session of the virtual user that has finished running
	 */
	def execute(session: Session) {

		DataWriter.endUser(session.scenarioName, session.userId)
		Terminator.endUser
		info("Done user #" + session.userId)
	}
}

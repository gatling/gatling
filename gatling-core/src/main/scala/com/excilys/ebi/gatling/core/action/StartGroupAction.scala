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
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.core.session.Session

import akka.actor.ActorRef
import akka.actor.actorRef2Scala

object StartGroupAction {
	val START_OF_GROUP = "Start of group"

	def startOfGroup(name: String) = START_OF_GROUP + "(" + name + ")"
}

class StartGroupAction(groupName: EvaluatableString, val next: ActorRef) extends Action {

	def execute(session: Session) {
		val resoldedGroupName = try {
			groupName(session)
		} catch {
			case e => error("Group name resolution crashed", e); "no-group-name"
		}

		DataWriter.startGroup(session.scenarioName, resoldedGroupName, session.userId)
		next ! session
	}
}

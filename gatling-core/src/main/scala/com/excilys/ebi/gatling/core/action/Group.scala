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
import com.excilys.ebi.gatling.core.session.{ Expression, Session }

import akka.actor.ActorRef
import scalaz.{ Failure, Success }

class Group(groupName: Expression[String], event: String, val next: ActorRef) extends Action {

	def execute(session: Session) {
		val resolvedGroupName = groupName(session) match {
			case Success(name) => name
			case Failure(message) => error("Could not resolve group name: " + message); "no-group-name"
		}

		DataWriter.group(session.scenarioName, resolvedGroupName, session.userId, event)
		next ! session
	}
}
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
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis

object GroupEnd extends DataWriterClient {

	def endGroup(session: Session, next: ActorRef) {
		val stack = session.groupStack
		writeGroupData(session, stack, stack.head.startDate, nowMillis, session.statusStack.head)

		next ! session.exitGroup
	}
}

class GroupEnd(val next: ActorRef) extends Chainable {

	def execute(session: Session) {
		GroupEnd.endGroup(session, next)
	}
}

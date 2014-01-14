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
import akka.actor.ActorDSL.actor
import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.controller.Controller
import io.gatling.core.result.message.End
import io.gatling.core.result.writer.UserMessage
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis

object UserEnd extends AkkaDefaults {

	var _instance: Option[ActorRef] = None

	def start() {
		_instance = Some(actor(new UserEnd))
		system.registerOnTermination(_instance = None)
	}

	def instance() = _instance match {
		case Some(a) => a
		case None => throw new UnsupportedOperationException("UserEnd hasn't been initialized")
	}
}

class UserEnd extends Action {

	def execute(session: Session) {
		Controller.instance ! UserMessage(session.scenarioName, session.userId, End, session.startDate, nowMillis)
	}
}

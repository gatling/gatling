/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 * Copyright 2012 Gilt Groupe, Inc. (www.gilt.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.ws

import com.ning.http.client.websocket.WebSocket

import akka.actor.ActorRef
import io.gatling.core.action.{ Action, Chainable, Failable }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.config.HttpProtocol

class SendWebSocketMessageAction(requestName: Expression[String], wsName: String, message: Expression[String], val next: ActorRef, protocol: HttpProtocol) extends Action with Chainable with Failable {

	def executeOrFail(session: Session) = {

		def send(requestName: String, message: String) {
			session(wsName).asOption[(ActorRef, WebSocket)].foreach(_._1 ! SendMessage(requestName, message, next, session))
		}

		for {
			requestName <- requestName(session)
			message <- message(session)
		} yield send(requestName, message)
	}
}

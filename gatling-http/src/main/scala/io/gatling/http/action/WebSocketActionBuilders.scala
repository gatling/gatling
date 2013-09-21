/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 * Copyright 2012 Gilt Groupe, Inc. (www.gilt.com)
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
package io.gatling.http.action

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.ProtocolRegistry
import io.gatling.core.session.Expression
import io.gatling.http.config.HttpProtocol
import io.gatling.http.util.{ RequestLogger, WebSocketClient }

class OpenWebSocketActionBuilder(actionName: Expression[String], attributeName: String, fUrl: Expression[String], webSocketClient: WebSocketClient, requestLogger: RequestLogger) extends ActionBuilder {

	def build(next: ActorRef, protocolRegistry: ProtocolRegistry): ActorRef = {
		val httpProtocol = protocolRegistry.getProtocol(HttpProtocol.default)
		actor(new OpenWebSocketAction(actionName, attributeName, fUrl, webSocketClient, requestLogger, next, httpProtocol))
	}
}

class SendWebSocketMessageActionBuilder(actionName: Expression[String], attributeName: String, fMessage: Expression[String], next: ActorRef = null) extends ActionBuilder {

	def build(next: ActorRef, protocolRegistry: ProtocolRegistry): ActorRef = {
		val httpProtocol = protocolRegistry.getProtocol(HttpProtocol.default)
		actor(new SendWebSocketMessageAction(actionName, attributeName, fMessage, next, httpProtocol))
	}
}

class CloseWebSocketActionBuilder(actionName: Expression[String], attributeName: String, next: ActorRef = null) extends ActionBuilder {

	def build(next: ActorRef, protocolRegistry: ProtocolRegistry): ActorRef = {
		val httpProtocol = protocolRegistry.getProtocol(HttpProtocol.default)
		actor(new CloseWebSocketAction(actionName, attributeName, next, httpProtocol))
	}
}

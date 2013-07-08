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
package io.gatling.http.request.builder

import io.gatling.core.session.Expression
import io.gatling.http.action.{ CloseWebSocketActionBuilder, OpenWebSocketActionBuilder, SendWebSocketMessageActionBuilder }
import io.gatling.http.util.{ RequestLogger, WebSocketClient }

/**
 * @param actionName The action name in the log
 */
class WebSocketBaseBuilder(val actionName: Expression[String]) {
	private val DEFAULT_ATTRIBUTE_NAME = "io.gatling.http.request.builder.WebSocket"

	/**
	 * Opens a web socket and stores it in the session.
	 *
	 * @param fUrl The socket URL
	 * @param attributeName The name of the session attribute used to store the socket
	 */
	def open(fUrl: Expression[String], attributeName: String = DEFAULT_ATTRIBUTE_NAME)(implicit webSocketClient: WebSocketClient, requestLogger: RequestLogger) = new OpenWebSocketActionBuilder(actionName, attributeName, fUrl, webSocketClient, requestLogger)

	/**
	 * Sends a message on the given socket.
	 *
	 * @param fMessage The message
	 * @param attributeName The name of the session attribute storing the socket
	 */
	def sendMessage(fMessage: Expression[String], attributeName: String = DEFAULT_ATTRIBUTE_NAME) = new SendWebSocketMessageActionBuilder(actionName, attributeName, fMessage)

	/**
	 * Closes a web socket.
	 *
	 * @param attributeName The name of the session attribute storing the socket
	 */
	def close(attributeName: String = DEFAULT_ATTRIBUTE_NAME) = new CloseWebSocketActionBuilder(actionName, attributeName)
}

object WebSocketBaseBuilder {
	def websocket(actionName: Expression[String]) = new WebSocketBaseBuilder(actionName)
}
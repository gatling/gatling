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
package io.gatling.http.util

import java.io.IOException
import java.net.URI

import com.ning.http.client.websocket.{ WebSocketListener, WebSocketUpgradeHandler }
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.result.message.Status
import io.gatling.core.result.writer.{ DataWriter, RequestMessage }
import io.gatling.core.session.Session
import io.gatling.http.ahc.HttpEngine

trait WebSocketClient {
	@throws(classOf[IOException])
	def open(uri: URI, listener: WebSocketListener)
}

trait RequestLogger {
	def logRequest(session: Session, actionName: String, status: Status, started: Long, ended: Long, errorMessage: Option[String] = None)
}

/** The default AsyncHttpClient WebSocket client. */
object DefaultWebSocketClient extends WebSocketClient with StrictLogging {
	def open(uri: URI, listener: WebSocketListener) {
		HttpEngine.instance.defaultAHC.prepareGet(uri.toString).execute(
			new WebSocketUpgradeHandler.Builder().addWebSocketListener(listener).build())
	}
}

/** The default Gatling request logger. */
object DefaultRequestLogger extends RequestLogger {
	def logRequest(session: Session, actionName: String, status: Status, started: Long, ended: Long, errorMessage: Option[String]) {
		DataWriter.tell(RequestMessage(
			session.scenarioName,
			session.userId,
			Nil,
			actionName,
			started,
			ended,
			ended,
			ended,
			status,
			errorMessage,
			Nil))
	}
}

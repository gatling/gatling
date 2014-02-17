/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.excilys.com)
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
import io.gatling.core.akka.BaseActor
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.writer.DataWriter
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.result.writer.DataWriterClient

class WebSocketActor(val wsName: String) extends BaseActor with DataWriterClient {

	private[this] var webSocket: Option[WebSocket] = None

	def logRequest(session: Session, requestName: String, status: Status, started: Long, ended: Long, errorMessage: Option[String] = None) {
		writeRequestData(
			session,
			requestName,
			started,
			ended,
			ended,
			ended,
			status,
			errorMessage)
	}

	def receive = {
		case OnOpen(requestName, webSocket, started, ended, next, session) =>
			logRequest(session, requestName, OK, started, ended)
			this.webSocket = Some(webSocket)
			next ! session.set(wsName, self)

		case OnFailedOpen(requestName, message, started, ended, next, session) =>
			logger.warn(s"Websocket '$wsName' failed to open: $message")
			logRequest(session, requestName, KO, started, ended, Some(message))
			next ! session.markAsFailed
			context.stop(self)

		case OnMessage(message) =>
			logger.debug(s"Received message on websocket '$wsName':$message")

		case OnClose =>
			setOutOfBandError(s"Websocket '$wsName' was unexpectedly closed")

		case OnError(t) =>
			setOutOfBandError(s"Websocket '$wsName' gave an error: '${t.getMessage}'")

		case SendTextMessage(requestName, message, next, session) =>
			if (!handleOutOfBandError(requestName, next, session)) {
				val started = nowMillis
				webSocket.foreach(_.sendTextMessage(message))
				logRequest(session, requestName, OK, started, nowMillis)
				next ! session
			}

		case SendBinaryMessage(requestName, message, next, session) =>
			if (!handleOutOfBandError(requestName, next, session)) {
				val started = nowMillis
				webSocket.foreach(_.sendMessage(message))
				logRequest(session, requestName, OK, started, nowMillis)
				next ! session
			}

		case Close(requestName, next, session) =>
			if (!handleOutOfBandError(requestName, next, session)) {
				val started = nowMillis
				webSocket.foreach(_.close)
				logRequest(session, requestName, OK, started, nowMillis)
				next ! session
				context.stop(self)
			}
	}

	private[this] var outOfBandError: Option[String] = None

	private[this] def setOutOfBandError(message: String) {
		outOfBandError = Some(message)
		logger.warn(message)
	}

	private[this] def handleOutOfBandError(requestName: String, next: ActorRef, session: Session) = {
		if (outOfBandError.isDefined) {
			val now = nowMillis
			logRequest(session, requestName, KO, now, now, outOfBandError)
			outOfBandError = None
			next ! session.markAsFailed
			context.stop(self)
			true
		} else
			false
	}
}

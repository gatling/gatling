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

import scala.collection.mutable.Queue

import com.ning.http.client.websocket.WebSocket

import akka.actor.ActorRef
import io.gatling.core.akka.BaseActor
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.http.ahc.{ HttpEngine, WebSocketTx }

class WebSocketActor(wsName: String) extends BaseActor with DataWriterClient {

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

	def sendTextMessage(webSocket: WebSocket, requestName: String, message: String, session: Session) {
		val started = nowMillis
		webSocket.sendTextMessage(message)
		logRequest(session, requestName, OK, started, nowMillis)
	}

	def sendBinaryMessage(webSocket: WebSocket, requestName: String, message: Array[Byte], session: Session) {
		val started = nowMillis
		webSocket.sendMessage(message)
		logRequest(session, requestName, OK, started, nowMillis)
	}

	def webSocketOpen(webSocket: WebSocket, tx: WebSocketTx): Receive = {
		case OnMessage(message) =>
			// TODO deal with messages
			logger.debug(s"Received message on websocket '$wsName':$message")

		case SendTextMessage(requestName, message, next, session) =>
			sendTextMessage(webSocket, requestName, message, session)
			next ! session

		case SendBinaryMessage(requestName, message, next, session) =>
			sendBinaryMessage(webSocket, requestName, message, session)
			next ! session

		case Close(requestName, next, session) =>
			val started = nowMillis
			webSocket.close
			logRequest(session, requestName, OK, started, nowMillis)
			next ! session.remove(wsName)
			context.become(closing)

		case OnUnexpectedClose | OnClose =>
			if (tx.protocol.wsPart.reconnect)
				if (tx.protocol.wsPart.maxReconnects.map(_ > tx.reconnectCount).getOrElse(true))
					context.become(disconnected(Queue.empty[WebSocketMessage], tx))
				else
					context.become(pendingErrorMessage(s"Websocket '$wsName' was unexpectedly closed and max reconnect reached"))

			else
				context.become(pendingErrorMessage(s"Websocket '$wsName' was unexpectedly closed"))

		case OnError(t) =>
			context.become(pendingErrorMessage(s"Websocket '$wsName' gave an error: '${t.getMessage}'"))
	}

	def closing: Receive = {
		case OnClose => context.stop(self)
	}

	def disconnected(pendingSendMessages: Queue[WebSocketMessage], tx: WebSocketTx): Receive = {

		case message: WebSocketMessage =>
			// reconnect on first client message tentative
			HttpEngine.instance.startWebSocketTransaction(tx.copy(reconnectCount = tx.reconnectCount + 1), self)

			context.become(reconnecting(pendingSendMessages += message))
	}

	def reconnecting(pendingSendMessages: Queue[WebSocketMessage]): Receive = {

		case message: WebSocketMessage =>
			pendingSendMessages += message

		case OnOpen(tx, webSocket, started, ended) =>
			// send all pending messages
			pendingSendMessages.foreach { self ! _ }

			context.become(webSocketOpen(webSocket, tx))

		case OnFailedOpen(tx, message, _, _) =>

			val error = s"Websocket '$wsName' failed to reconnect: $message"

			// send all pending messages
			pendingSendMessages.foreach { self ! _ }

			context.become(pendingErrorMessage(error))
	}

	def pendingErrorMessage(error: String): Receive = {

		def flushPendingError(requestName: String, next: ActorRef, session: Session) {
			val now = nowMillis
			logRequest(session, requestName, KO, now, now, Some(error))
			next ! session.markAsFailed.remove(wsName)
			context.stop(self)
		}

		{
			case SendTextMessage(requestName, _, next, session) =>
				flushPendingError(requestName, next, session)

			case SendBinaryMessage(requestName, _, next, session) =>
				flushPendingError(requestName, next, session)

			case Close(requestName, next, session) =>
				flushPendingError(requestName, next, session)
		}
	}

	def receive = {
		case OnOpen(tx, webSocket, started, ended) =>
			import tx._
			logRequest(session, requestName, OK, started, ended)
			next ! session.set(wsName, self)
			context.become(webSocketOpen(webSocket, tx))

		case OnFailedOpen(tx, message, started, ended) =>
			import tx._
			logger.warn(s"Websocket '$wsName' failed to open: $message")
			logRequest(session, requestName, KO, started, ended, Some(message))
			next ! session.markAsFailed
			context.stop(self)
	}
}

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

import java.io.IOException
import java.net.URI

import com.ning.http.client.websocket.{ WebSocket, WebSocketTextListener }

import akka.actor.ActorRef
import akka.actor.ActorDSL.actor
import io.gatling.core.action.{ Action, Chainable, Failable, Interruptable }
import io.gatling.core.akka.BaseActor
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.util.{ RequestLogger, WebSocketClient }

private[http] class OpenWebSocketAction(
	actionName: Expression[String],
	attributeName: String,
	url: Expression[String],
	webSocketClient: WebSocketClient,
	requestLogger: RequestLogger,
	val next: ActorRef,
	protocol: HttpProtocol) extends Interruptable with Failable {

	def makeAbsolute(url: String): Validation[String] =
		if (url.startsWith("ws"))
			url.success
		else
			protocol.wsPart.wsBaseURL match {
				case Some(baseURL) => (baseURL + url).success
				case _ => s"No protocol.wsBaseURL defined but provided url is relative : $url".failure
			}

	def executeOrFail(session: Session) = {

		def open(actionName: String, url: String) = {
			logger.info(s"Opening websocket '$attributeName': Scenario '${session.scenarioName}', UserId #${session.userId}")

			val wsActor = actor(context)(new WebSocketActor(attributeName, requestLogger))

			val started = nowMillis
			try {
				webSocketClient.open(URI.create(url), new WebSocketTextListener {
					var opened = false

					def onOpen(webSocket: WebSocket) {
						opened = true
						wsActor ! OnOpen(actionName, webSocket, started, nowMillis, next, session)
					}

					def onMessage(message: String) {
						wsActor ! OnMessage(message)
					}

					def onFragment(fragment: String, last: Boolean) {
					}

					def onClose(webSocket: WebSocket) {
						if (opened) {
							opened = false
							wsActor ! OnClose
						} else {
							wsActor ! OnFailedOpen(actionName, "closed", started, nowMillis, next, session)
						}
					}

					def onError(t: Throwable) {
						if (opened) {
							wsActor ! OnError(t)
						} else {
							wsActor ! OnFailedOpen(actionName, t.getMessage, started, nowMillis, next, session)
						}
					}
				})
			} catch {
				case e: IOException =>
					wsActor ! OnFailedOpen(actionName, e.getMessage, started, nowMillis, next, session)
			}
		}

		for {
			actionName <- actionName(session)
			url <- url(session)
			absoluteUrl <- makeAbsolute(url)
		} yield open(actionName, absoluteUrl)
	}
}

private[http] class SendWebSocketMessageAction(actionName: Expression[String], attributeName: String, message: Expression[String], val next: ActorRef, protocol: HttpProtocol) extends Action with Chainable with Failable {

	def executeOrFail(session: Session) = {

		def send(actionName: String, message: String) {
			session(attributeName).asOption[(ActorRef, WebSocket)].foreach(_._1 ! SendMessage(actionName, message, next, session))
		}

		for {
			actionName <- actionName(session)
			message <- message(session)
		} yield send(actionName, message)
	}
}

private[http] class CloseWebSocketAction(actionName: Expression[String], attributeName: String, val next: ActorRef, protocol: HttpProtocol) extends Action with Chainable with Failable {

	def executeOrFail(session: Session) = {

		def close(actionName: String) {
			logger.info(s"Closing websocket '$attributeName': Scenario '${session.scenarioName}', UserId #${session.userId}")
			session(attributeName).asOption[(ActorRef, WebSocket)].foreach(_._1 ! Close(actionName, next, session))
		}

		actionName(session).map(close)
	}
}

private[http] class WebSocketActor(val attributeName: String, requestLogger: RequestLogger) extends BaseActor {

	private[this] var webSocket: Option[WebSocket] = None

	def receive = {
		case OnOpen(actionName, webSocket, started, ended, next, session) =>
			requestLogger.logRequest(session, actionName, OK, started, ended)
			this.webSocket = Some(webSocket)
			next ! session.set(attributeName, (self, webSocket))

		case OnFailedOpen(actionName, message, started, ended, next, session) =>
			logger.warn(s"Websocket '$attributeName' failed to open: $message")
			requestLogger.logRequest(session, actionName, KO, started, ended, Some(message))
			next ! session.markAsFailed
			context.stop(self)

		case OnMessage(message) =>
			logger.debug(s"Received message on websocket '$attributeName':\n$message")

		case OnClose =>
			setOutOfBandError(s"Websocket '$attributeName' was unexpectedly closed")

		case OnError(t) =>
			setOutOfBandError(s"Websocket '$attributeName' gave an error: '${t.getMessage}'")

		case SendMessage(actionName, message, next, session) =>
			if (!handleOutOfBandError(actionName, next, session)) {
				val started = nowMillis
				webSocket.foreach(_.sendTextMessage(message))
				requestLogger.logRequest(session, actionName, OK, started, nowMillis)
				next ! session
			}

		case Close(actionName, next, session) =>
			if (!handleOutOfBandError(actionName, next, session)) {
				val started = nowMillis
				webSocket.foreach(_.close)
				requestLogger.logRequest(session, actionName, OK, started, nowMillis)
				next ! session
				context.stop(self)
			}
	}

	private[this] var outOfBandError: Option[String] = None

	private[this] def setOutOfBandError(message: String) {
		outOfBandError = Some(message)
		logger.warn(message)
	}

	private[this] def handleOutOfBandError(actionName: String, next: ActorRef, session: Session) = {
		if (outOfBandError.isDefined) {
			val now = nowMillis
			requestLogger.logRequest(session, actionName, KO, now, now, outOfBandError)
			outOfBandError = None
			next ! session.markAsFailed
			context.stop(self)
			true
		} else {
			false
		}
	}
}

private[http] case class OnOpen(actionName: String, webSocket: WebSocket, started: Long, ended: Long, next: ActorRef, session: Session)
private[http] case class OnFailedOpen(actionName: String, message: String, started: Long, ended: Long, next: ActorRef, session: Session)
private[http] case class OnMessage(message: String)
private[http] case object OnClose
private[http] case class OnError(t: Throwable)

private[http] case class SendMessage(actionName: String, message: String, next: ActorRef, session: Session)
private[http] case class Close(actionName: String, next: ActorRef, session: Session)

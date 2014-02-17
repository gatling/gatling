/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.http.action.ws

import com.ning.http.client.{ AsyncHttpClient, Request }
import com.ning.http.client.websocket.{ WebSocket, WebSocketTextListener => AHCWebSocketTextListener, WebSocketUpgradeHandler }

import akka.actor.ActorRef
import akka.actor.ActorDSL.actor
import io.gatling.core.action.{ Failable, Interruptable }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.config.HttpProtocol

class WebSocketTextListener(requestName: String, request: Request, session: Session, httpClient: AsyncHttpClient, wsActor: ActorRef, started: Long, next: ActorRef)
	extends AHCWebSocketTextListener {
	var opened = false

	def onOpen(webSocket: WebSocket) {
		opened = true
		wsActor ! OnOpen(requestName, webSocket, started, nowMillis, next, session)
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
		} else
			wsActor ! OnFailedOpen(requestName, "closed", started, nowMillis, next, session)
	}

	def onError(t: Throwable) {
		if (opened)
			wsActor ! OnError(t)
		else
			wsActor ! OnFailedOpen(requestName, t.getMessage, started, nowMillis, next, session)
	}
}

class OpenWebSocketAction(
	requestName: Expression[String],
	wsName: String,
	request: Expression[Request],
	val next: ActorRef,
	protocol: HttpProtocol) extends Interruptable {

	def execute(session: Session) {

		def open(requestName: String, request: Request, session: Session, httpClient: AsyncHttpClient) {
			logger.info(s"Opening websocket '$wsName': Scenario '${session.scenarioName}', UserId #${session.userId}")

			val wsActor = actor(context)(new WebSocketActor(wsName))

			val started = nowMillis
			try {
				val listener = new WebSocketTextListener(requestName, request, session, httpClient, wsActor, started, next)

				val handler = new WebSocketUpgradeHandler.Builder().addWebSocketListener(listener).build
				httpClient.executeRequest(request, handler)

			} catch {
				case e: Exception =>
					wsActor ! OnFailedOpen(requestName, e.getMessage, started, nowMillis, next, session)
			}
		}

		val (sessionWithHttpClient, httpClient) = HttpEngine.instance.httpClient(session, protocol)

		for {
			requestName <- requestName(sessionWithHttpClient)
			request <- request(session)
		} yield open(requestName, request, sessionWithHttpClient, httpClient)
	}
}

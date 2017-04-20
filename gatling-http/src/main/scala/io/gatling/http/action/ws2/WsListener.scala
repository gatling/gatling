/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.ws2

import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.http.action.ws2.fsm.{ TextMessageReceived, WebSocketClosed, WebSocketCrashed, WebSocketOpened }

import akka.actor.ActorRef
import org.asynchttpclient.ws.{ WebSocket, WebSocketListener }

class WsListener(wsActor: ActorRef) extends WebSocketListener {

  override def onOpen(webSocket: WebSocket): Unit =
    wsActor ! WebSocketOpened(webSocket, nowMillis)

  override def onClose(webSocket: WebSocket, code: Int, reason: String): Unit =
    wsActor ! WebSocketClosed(code, reason, nowMillis)

  override def onTextFrame(message: String, finalFragment: Boolean, rsv: Int): Unit =
    wsActor ! TextMessageReceived(message, nowMillis)

  override def onError(t: Throwable): Unit =
    wsActor ! WebSocketCrashed(t, nowMillis)
}

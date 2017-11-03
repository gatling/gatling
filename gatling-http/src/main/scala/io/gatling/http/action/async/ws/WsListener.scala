/*
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
package io.gatling.http.action.async.ws

import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.http.action.async.{ AsyncTx, OnFailedOpen }

import akka.actor.ActorRef
import com.typesafe.scalalogging.LazyLogging
import org.asynchttpclient.ws._

class WsListener(tx: AsyncTx, wsActor: ActorRef) extends WebSocketListener with LazyLogging {

  private var state: WsListenerState = Opening
  private var webSocket: WebSocket = _

  override def onOpen(webSocket: WebSocket): Unit = {
    state = Open
    this.webSocket = webSocket
    wsActor ! OnOpen(tx, webSocket, nowMillis)
  }

  override def onError(t: Throwable): Unit =
    state match {
      case Opening =>
        wsActor ! OnFailedOpen(tx, t.getMessage, nowMillis)

      case _ =>
        logger.warn(s"WebSocket unexpected error '${t.getMessage}'", t)
    }

  override def onClose(webSocket: WebSocket, statusCode: Int, reason: String): Unit = {
    state match {
      case Open =>
        state = Closed
        wsActor ! OnClose(statusCode, reason, nowMillis)

      case _ => // discard
    }
  }

  override def onTextFrame(message: String, finalFragment: Boolean, rsv: Int): Unit =
    wsActor ! OnTextMessage(message, nowMillis)

  override def onBinaryFrame(message: Array[Byte], finalFragment: Boolean, rsv: Int): Unit =
    wsActor ! OnByteMessage(message, nowMillis)

  override def onPingFrame(message: Array[Byte]): Unit =
    webSocket.sendPongFrame(message)
}

private sealed trait WsListenerState
private case object Opening extends WsListenerState
private case object Open extends WsListenerState
private case object Closed extends WsListenerState

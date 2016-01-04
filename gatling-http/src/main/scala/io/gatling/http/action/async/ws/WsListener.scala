/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.http.action.async.{ AsyncTx, OnFailedOpen }

import akka.actor.ActorRef
import com.typesafe.scalalogging.LazyLogging
import org.asynchttpclient.ws._

class WsListener(tx: AsyncTx, wsActor: ActorRef)
    extends WebSocketTextListener
    with WebSocketByteListener
    with WebSocketCloseCodeReasonListener
    with WebSocketPingListener
    with LazyLogging {

  private var state: WsListenerState = Opening
  private var webSocket: WebSocket = _

  // WebSocketListener
  def onOpen(webSocket: WebSocket): Unit = {
    state = Open
    this.webSocket = webSocket
    wsActor ! OnOpen(tx, webSocket, nowMillis)
  }

  def onClose(webSocket: WebSocket): Unit = ()

  def onError(t: Throwable): Unit =
    state match {
      case Opening =>
        wsActor ! OnFailedOpen(tx, t.getMessage, nowMillis)

      case _ =>
        logger.warn(s"WebSocket unexpected error '${t.getMessage}'", t)
    }

  // WebSocketCloseCodeReasonListener
  def onClose(webSocket: WebSocket, statusCode: Int, reason: String): Unit = {
    state match {
      case Open =>
        state = Closed
        wsActor ! OnClose(statusCode, reason, nowMillis)

      case _ => // discard
    }
  }

  // WebSocketTextListener
  def onMessage(message: String): Unit =
    wsActor ! OnTextMessage(message, nowMillis)

  // WebSocketByteListener
  def onMessage(message: Array[Byte]): Unit =
    wsActor ! OnByteMessage(message, nowMillis)

  // WebSocketPingListener
  def onPing(message: Array[Byte]): Unit =
    webSocket.sendPong(message)
}

private sealed trait WsListenerState
private case object Opening extends WsListenerState
private case object Open extends WsListenerState
private case object Closed extends WsListenerState

/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.ws

import com.ning.http.client.websocket.{ WebSocket, WebSocketCloseCodeReasonListener, WebSocketTextListener }

import akka.actor.ActorRef
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.http.ahc.WsTx
import com.typesafe.scalalogging.slf4j.StrictLogging

class WsListener(tx: WsTx, wsActor: ActorRef)
    extends WebSocketTextListener with WebSocketCloseCodeReasonListener with StrictLogging {

  private var state: WsListenerState = Opening

  def onOpen(webSocket: WebSocket): Unit = {
    state = Open
    wsActor ! OnOpen(tx, webSocket, nowMillis)
  }

  def onMessage(message: String): Unit = {
    wsActor ! OnMessage(message, nowMillis)
  }

  def onFragment(fragment: String, last: Boolean): Unit = {}

  def onClose(webSocket: WebSocket): Unit = {}

  def onClose(webSocket: WebSocket, statusCode: Int, reason: String): Unit = {
    state match {
      case Open =>
        state = Closed
        wsActor ! OnClose(statusCode, reason, nowMillis)

      case _ => // discard
    }
  }

  def onError(t: Throwable): Unit = {
    state match {
      case Opening =>
        wsActor ! OnFailedOpen(tx, t.getMessage, nowMillis)

      case Open =>
        logger.error(s"Websocket gave an unexpected error '${t.getMessage}', please report to Gatling project", t)

      case Closed => // discard
    }
  }
}

private sealed trait WsListenerState

private case object Opening extends WsListenerState

private case object Open extends WsListenerState

private case object Closed extends WsListenerState

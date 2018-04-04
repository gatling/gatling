/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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
import io.gatling.http.client.WebSocketListener
import io.gatling.netty.util.ahc.Utf8ByteBufCharsetDecoder._

import akka.actor.ActorRef
import com.typesafe.scalalogging.LazyLogging
import io.netty.buffer.ByteBufUtil
import io.netty.handler.codec.http.websocketx.{ BinaryWebSocketFrame, CloseWebSocketFrame, PongWebSocketFrame, TextWebSocketFrame }
import io.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }

class WsListener(tx: AsyncTx, wsActor: ActorRef) extends WebSocketListener with LazyLogging {

  private var state: WsListenerState = Opening

  override def onBinaryFrame(frame: BinaryWebSocketFrame): Unit =
    wsActor ! OnByteMessage(ByteBufUtil.getBytes(frame.content), nowMillis)

  override def onPongFrame(frame: PongWebSocketFrame): Unit =
    logger.debug("Received PONG frame")

  override def onCloseFrame(frame: CloseWebSocketFrame): Unit =
    state match {
      case Open =>
        state = Closed
        wsActor ! OnClose(frame.statusCode, frame.reasonText, nowMillis)
      case _ => // discard
    }

  override def onTextFrame(textWebSocketFrame: TextWebSocketFrame): Unit =
    wsActor ! OnTextMessage(decodeUtf8(textWebSocketFrame.content()), nowMillis)

  override def onWebSocketOpen(): Unit = {
    state = Open
    wsActor ! OnOpen(tx, this, nowMillis)
  }

  override def onHttpResponse(httpResponseStatus: HttpResponseStatus, httpHeaders: HttpHeaders): Unit =
    logger.debug(s"Received response to WebSocket CONNECT: $httpResponseStatus $httpHeaders")

  override def onThrowable(t: Throwable): Unit =
    state match {
      case Opening =>
        wsActor ! OnFailedOpen(tx, t.getMessage, nowMillis)

      case _ =>
        logger.warn(s"WebSocket unexpected error '${t.getMessage}'", t)
    }
}

private sealed trait WsListenerState
private case object Opening extends WsListenerState
private case object Open extends WsListenerState
private case object Closed extends WsListenerState

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

package io.gatling.http.action.ws2

import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.core.stats.StatsEngine
import io.gatling.http.action.ws2.fsm._
import io.gatling.http.client.WebSocketListener
import io.gatling.netty.util.ahc.{ ByteBufUtils, Utf8ByteBufCharsetDecoder }

import akka.actor.ActorRef
import com.typesafe.scalalogging.LazyLogging
import io.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }
import io.netty.handler.codec.http.websocketx.{ BinaryWebSocketFrame, CloseWebSocketFrame, PongWebSocketFrame, TextWebSocketFrame }

class WsListener(wsActor: ActorRef, statsEngine: StatsEngine) extends WebSocketListener with LazyLogging {

  //[fl]
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //[fl]

  override def onHttpResponse(httpResponseStatus: HttpResponseStatus, httpHeaders: HttpHeaders): Unit =
    logger.debug(s"Received response to WebSocket CONNECT: $httpResponseStatus $httpHeaders")

  override def onWebSocketOpen(): Unit =
    wsActor ! WebSocketOpened(this, nowMillis)

  override def onCloseFrame(frame: CloseWebSocketFrame): Unit =
    wsActor ! WebSocketClosed(frame.statusCode, frame.reasonText, nowMillis)

  override def onTextFrame(frame: TextWebSocketFrame): Unit =
    wsActor ! TextFrameReceived(Utf8ByteBufCharsetDecoder.decodeUtf8(frame.content()), nowMillis)

  override def onBinaryFrame(frame: BinaryWebSocketFrame): Unit =
    wsActor ! BinaryFrameReceived(ByteBufUtils.byteBuf2Bytes(frame.content()), nowMillis)

  override def onPongFrame(pongWebSocketFrame: PongWebSocketFrame): Unit =
    logger.debug("Received PONG frame")

  override def onThrowable(t: Throwable): Unit =
    wsActor ! WebSocketCrashed(t, nowMillis)
}

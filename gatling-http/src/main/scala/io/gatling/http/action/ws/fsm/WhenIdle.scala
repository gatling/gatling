/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.ws.fsm

import io.gatling.commons.stats.OK
import io.gatling.http.check.ws.WsFrameCheckSequence

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.websocketx.{ BinaryWebSocketFrame, CloseWebSocketFrame, TextWebSocketFrame }

trait WhenIdle { this: WsActor =>

  when(Idle) {
    case Event(SendTextFrame(actionName, message, checkSequences, session, nextAction), IdleData(_, webSocket)) =>
      logger.debug(s"Send text frame $actionName $message")
      // actually send message!
      val now = clock.nowMillis
      webSocket.sendFrame(new TextWebSocketFrame(message))

      configuration.resolve(
        // [fl]
        //
        // [fl]
        statsEngine.logResponse(session, actionName, now, now, OK, None, None)
      )

      checkSequences match {
        case WsFrameCheckSequence(timeout, currentCheck :: remainingChecks) :: remainingCheckSequences =>
          logger.debug("Trigger check after sending text frame")
          val timeoutId = scheduleTimeout(timeout)
          //[fl]
          //
          //[fl]
          goto(PerformingCheck) using PerformingCheckData(
            webSocket = webSocket,
            currentCheck = currentCheck,
            remainingChecks = remainingChecks,
            checkSequenceStart = now,
            checkSequenceTimeoutId = timeoutId,
            remainingCheckSequences,
            session = session,
            next = Left(nextAction)
          )

        case _ => // same as Nil as WsFrameCheckSequence#checks can't be Nil, but compiler complains that match may not be exhaustive
          nextAction ! session
          stay()
      }

    case Event(SendBinaryFrame(actionName, message, checkSequences, session, nextAction), IdleData(_, webSocket)) =>
      logger.debug(s"Send binary frame $actionName length=${message.length}")
      // actually send message!
      val now = clock.nowMillis
      webSocket.sendFrame(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(message)))

      configuration.resolve(
        // [fl]
        //
        // [fl]
        statsEngine.logResponse(session, actionName, now, now, OK, None, None)
      )

      checkSequences match {
        case WsFrameCheckSequence(timeout, currentCheck :: remainingChecks) :: remainingCheckSequences =>
          logger.debug("Trigger check after sending binary frame")
          val timeoutId = scheduleTimeout(timeout)
          //[fl]
          //
          //[fl]
          goto(PerformingCheck) using PerformingCheckData(
            webSocket = webSocket,
            currentCheck = currentCheck,
            remainingChecks = remainingChecks,
            checkSequenceStart = now,
            checkSequenceTimeoutId = timeoutId,
            remainingCheckSequences,
            session = session,
            next = Left(nextAction)
          )

        case _ => // same as Nil as WsFrameCheckSequence#checks can't be Nil, but compiler complains that match may not be exhaustive
          nextAction ! session
          stay()
      }

    case Event(_: FrameReceived, IdleData(session, _)) =>
      // server push message, just log
      logUnmatchedServerMessage(session)
      stay()

    case Event(WebSocketClosed(code, reason, _), _) =>
      // server issued close
      logger.info(s"WebSocket was forcefully closed ($code:$reason) by the server while in Idle state")
      goto(Crashed) using CrashedData(None)

    case Event(WebSocketCrashed(t, _), _) =>
      // crash
      logger.info("WebSocket crashed by the server while in Idle state", t)
      goto(Crashed) using CrashedData(Some(t.getMessage))

    case Event(ClientCloseRequest(name, session, next), IdleData(_, webSocket)) =>
      logger.info("Client requested WebSocket close")
      webSocket.sendFrame(new CloseWebSocketFrame())
      //[fl]
      //
      //[fl]
      goto(Closing) using ClosingData(name, session, next, clock.nowMillis) // TODO should we have a close timeout?
  }
}

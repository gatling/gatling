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
package io.gatling.http.action.ws2.fsm

import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.http.action.ws2.WsCheckSequence

trait WhenIdle { this: WsActor =>

  when(Idle) {
    case Event(SendTextMessage(actionName, message, checkSequences, session, nextAction), IdleData(_, webSocket)) =>
      logger.debug(s"Send message $actionName $message")
      // actually send message!
      val timestamp = nowMillis
      webSocket.sendTextFrame(message)

      //[fl]
      //
      //[fl]
      checkSequences match {
        case Nil =>
          nextAction ! session
          stay()

        case firstCheckSequence :: remainingCheckSequences =>
          logger.debug("Trigger check after send message")
          val timeoutId = scheduleTimeout(firstCheckSequence.timeout)
          //[fl]
          //
          //[fl]
          goto(PerformingCheck) using PerformingCheckData(
            webSocket = webSocket,
            currentCheck = firstCheckSequence.head,
            remainingChecks = firstCheckSequence.tail,
            checkSequenceStart = timestamp,
            checkSequenceTimeoutId = timeoutId,
            remainingCheckSequences: List[WsCheckSequence],
            session = session,
            next = Left(nextAction)
          )
      }

    case Event(TextMessageReceived(message, _), IdleData(session, _)) =>
      // server push message, just log
      logger.debug(s"Received push message $message")
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
      webSocket.sendCloseFrame()
      //[fl]
      //
      //[fl]
      goto(Closing) using ClosingData(name, session, next, nowMillis) // TODO should we have a close timeout?
  }
}

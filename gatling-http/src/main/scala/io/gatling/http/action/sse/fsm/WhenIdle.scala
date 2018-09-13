/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.sse.fsm

import io.gatling.http.check.sse.SseMessageCheckSequence

trait WhenIdle { this: SseActor =>

  when(Idle) {
    case Event(SetCheck(actionName, checkSequences, session, nextAction), IdleData(_, stream)) =>
      logger.debug(s"Sent check $actionName")
      // actually send message!
      val timestamp = clock.nowMillis

      checkSequences match {
        case SseMessageCheckSequence(timeout, currentCheck :: remainingChecks) :: remainingCheckSequences =>
          logger.debug("Trigger check after send message")
          val timeoutId = scheduleTimeout(timeout)
          //[fl]
          //
          //[fl]
          goto(PerformingCheck) using PerformingCheckData(
            stream = stream,
            currentCheck = currentCheck,
            remainingChecks = remainingChecks,
            checkSequenceStart = timestamp,
            checkSequenceTimeoutId = timeoutId,
            remainingCheckSequences,
            session = session,
            next = Left(nextAction)
          )

        case _ => // same as Nil as WsFrameCheckSequence#checks can't be Nil, but compiler complains that match may not be exhaustive
          nextAction ! session
          stay()
      }

    case Event(_: SseReceived, IdleData(session, _)) =>
      // server push message, just log
      logUnmatchedServerMessage(session)
      stay()

    case Event(SseStreamClosed(_), _) =>
      // server issued close
      logger.info(s"SSE stream was forcefully closed by the server while in Idle state")
      goto(Crashed) using CrashedData(None)

    case Event(SseStreamCrashed(t, _), _) =>
      // crash
      logger.info("SSE stream crashed by the server while in Idle state", t)
      goto(Crashed) using CrashedData(Some(t.getMessage))

    case Event(ClientCloseRequest(name, session, next), IdleData(_, stream)) =>
      logger.info("Client requested SSE stream close")
      stream.close()
      //[fl]
      //
      //[fl]
      goto(Closing) using ClosingData(name, session, next, clock.nowMillis)
  }
}

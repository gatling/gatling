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

package io.gatling.http.action.sse.fsm

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.commons.util.Throwables._
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.action.sse.SseListener
import io.gatling.http.check.sse.SseMessageCheckSequence

import io.netty.handler.codec.http.HttpResponseStatus.SWITCHING_PROTOCOLS

object WhenConnecting {

  val WsConnectSuccessStatusCode = Some(Integer.toString(SWITCHING_PROTOCOLS.code))
}

trait WhenConnecting { this: SseActor =>

  def gotoConnecting(session: Session, next: Either[Action, SetCheck], remainingTries: Int = httpProtocol.wsPart.maxReconnects.getOrElse(0)): State = {

    val listener = new SseListener(self, statsEngine)

    // [fl]
    //
    // [fl]
    httpEngine.httpClient.sendRequest(connectRequest, session.userId, httpProtocol.enginePart.shareConnections, listener)

    goto(Connecting) using ConnectingData(session, next, nowMillis, remainingTries)
  }

  private def handleConnectFailure(session: Session, next: Either[Action, SetCheck], connectStart: Long, connectEnd: Long, code: Option[String], reason: String, remainingTries: Int): State = {
    // log connect failure
    val newSession = logResponse(session, connectActionName, connectStart, connectEnd, KO, code, Some(reason))
    val newRemainingTries = remainingTries - 1
    if (newRemainingTries > 0) {
      // try again
      logger.debug(s"Connect failed: $code:$reason, retrying ($newRemainingTries remaining tries)")
      gotoConnecting(newSession, next, newRemainingTries)

    } else {
      val nextAction = next match {
        case Left(n) =>
          // failed to connect
          logger.debug(s"Connect failed: $code:$reason, no remaining tries, going to Crashed state and performing next action")
          n
        case Right(sendFrame) =>
          // failed to reconnect, logging failure to send message
          logger.debug(s"Connect failed: $code:$reason, no remaining tries, going to Crashed state, failing pending Send and performing next action")
          statsEngine.logCrash(newSession, sendFrame.actionName, "Failed to reconnect")
          sendFrame.next
      }
      nextAction ! newSession.markAsFailed
      goto(Crashed) using CrashedData(Some(reason))
    }
  }

  when(Connecting) {
    case Event(SseStreamConnected(stream, connectEnd), ConnectingData(session, next, connectStart, _)) =>
      val sessionWithGroupTimings = logResponse(session, connectActionName, connectStart, connectEnd, OK, WhenConnecting.WsConnectSuccessStatusCode, None)

      connectCheckSequence match {
        case SseMessageCheckSequence(timeout, currentCheck :: remainingChecks) :: remainingCheckSequences =>
          // wait for some checks before proceeding
          logger.debug("Connected, performing checks before proceeding")

          val timeoutId = scheduleTimeout(timeout)
          //[fl]
          //
          //[fl]
          goto(PerformingCheck) using PerformingCheckData(
            stream = stream,
            currentCheck = currentCheck,
            remainingChecks = remainingChecks,
            checkSequenceStart = connectEnd,
            checkSequenceTimeoutId = timeoutId,
            remainingCheckSequences = remainingCheckSequences,
            session = sessionWithGroupTimings,
            next = next
          )

        case _ => // same as Nil as WsFrameCheckSequence#checks can't be Nil, but compiler complains that match may not be exhaustive
          // send next
          next match {
            case Left(nextAction) =>
              logger.debug("Connected, no checks, performing next action")
              nextAction ! sessionWithGroupTimings

            case Right(sendFrame) =>
              logger.debug("Reconnected, no checks, sending pending message")
              self ! sendFrame.copyWithSession(sessionWithGroupTimings)
          }
          goto(Idle) using IdleData(sessionWithGroupTimings, stream)
      }

    case Event(SseStreamClosed(code, reason, timestamp), ConnectingData(session, next, connectStart, remainingTries)) =>
      // unexpected close
      handleConnectFailure(session, next, connectStart, timestamp, Some(String.valueOf(code)), reason, remainingTries)

    case Event(SseStreamCrashed(t, timestamp), ConnectingData(session, next, connectStart, remainingTries)) =>
      // crash
      handleConnectFailure(session, next, connectStart, timestamp, None, t.rootMessage, remainingTries)
  }
}


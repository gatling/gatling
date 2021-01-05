/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
import io.gatling.commons.util.Throwables._
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.check.sse.SseMessageCheckSequence

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.HttpResponseStatus

object SseConnectingState {

  private val SseConnectSuccessStatusCode = Some(Integer.toString(HttpResponseStatus.OK.code))

  def gotoConnecting(fsm: SseFsm, session: Session, next: Action): NextSseState = {
    fsm.stream.connect()
    NextSseState(new SseConnectingState(fsm, session, next, fsm.clock.nowMillis))
  }
}

class SseConnectingState(fsm: SseFsm, session: Session, next: Action, connectStart: Long) extends SseState(fsm) with StrictLogging {

  import fsm._

  override def onSseStreamConnected(connectEnd: Long): NextSseState = {
    val sessionWithGroupTimings = logResponse(session, connectActionName, connectStart, connectEnd, OK, SseConnectingState.SseConnectSuccessStatusCode, None)

    connectCheckSequence match {
      case SseMessageCheckSequence(timeout, currentCheck :: remainingChecks) :: remainingCheckSequences =>
        // wait for some checks before proceeding
        logger.debug("Connected, performing checks before proceeding")

        scheduleTimeout(timeout)
        //[fl]
        //
        //[fl]
        NextSseState(
          SsePerformingCheckState(
            fsm,
            currentCheck = currentCheck,
            remainingChecks = remainingChecks,
            checkSequenceStart = connectEnd,
            remainingCheckSequences = remainingCheckSequences,
            session = sessionWithGroupTimings,
            next = next
          )
        )

      case _ =>
        logger.debug("Connected, no checks, performing next action")
        next ! sessionWithGroupTimings
        NextSseState(new SseIdleState(fsm, sessionWithGroupTimings))
    }
  }

  override def onSseStreamCrashed(t: Throwable, timestamp: Long): NextSseState = {
    val error = t.rootMessage
    val newSession = logResponse(session, connectActionName, connectStart, timestamp, KO, None, Some(error))
    logger.debug(s"Connect failed: $error, going to Crashed state and performing next action")

    NextSseState(new SseCrashedState(fsm, error), () => next ! newSession.markAsFailed)
  }
}

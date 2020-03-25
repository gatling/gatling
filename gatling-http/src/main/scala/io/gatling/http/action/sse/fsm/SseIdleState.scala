/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

import com.typesafe.scalalogging.StrictLogging
import io.gatling.commons.util.Throwables._
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.check.sse.SseMessageCheckSequence

class SseIdleState(fsm: SseFsm, session: Session, stream: SseStream) extends SseState(fsm) with StrictLogging {

  import fsm._

  override def onSetCheck(actionName: String, checkSequences: List[SseMessageCheckSequence], session: Session, next: Action): NextSseState = {
    logger.debug(s"Sent check $actionName")
    // actually send message!
    val timestamp = clock.nowMillis

    checkSequences match {
      case SseMessageCheckSequence(timeout, currentCheck :: remainingChecks) :: remainingCheckSequences =>
        logger.debug("Trigger check after send message")
        scheduleTimeout(timeout)
        //[fl]
        //
        //[fl]
        NextSseState(
          SsePerformingCheckState(
            fsm,
            stream = stream,
            currentCheck = currentCheck,
            remainingChecks = remainingChecks,
            checkSequenceStart = timestamp,
            remainingCheckSequences,
            session = session,
            next = Left(next)
          )
        )

      case _ => // same as Nil as WsFrameCheckSequence#checks can't be Nil, but compiler complains that match may not be exhaustive
        NextSseState(
          this,
          () => next ! session
        )
    }
  }

  override def onSseReceived(message: String, timestamp: Long): NextSseState = {
    // server push message, just log
    logger.debug(s"Received unmatched message=$message")
    logUnmatchedServerMessage(session)
    NextSseState(this)
  }

  override def onSseStreamClosed(timestamp: Long): NextSseState = {
    // server issued close
    logger.info(s"SSE stream was forcefully closed by the server while in Idle state")
    NextSseState(new SseCrashedState(fsm, None))
  }

  override def onSseStreamCrashed(t: Throwable, timestamp: Long): NextSseState = {
    logger.info("SSE stream crashed by the server while in Idle state", t)
    NextSseState(new SseCrashedState(fsm, Some(t.rootMessage)))
  }
  override def onClientCloseRequest(actionName: String, session: Session, next: Action): NextSseState = {
    logger.info("Client requested SSE stream close")
    //[fl]
    //
    //[fl]
    NextSseState(new SseClosingState(fsm, actionName, session, next, clock.nowMillis), () => stream.close())
  }
}

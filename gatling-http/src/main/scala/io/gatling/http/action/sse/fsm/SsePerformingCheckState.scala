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
import io.gatling.commons.validation.{ Failure, Success }
import io.gatling.core.action.Action
import io.gatling.core.check.Check
import io.gatling.core.session.Session
import io.gatling.http.check.sse.{ SseCheck, SseMessageCheck, SseMessageCheckSequence }

import com.typesafe.scalalogging.StrictLogging

final case class SsePerformingCheckState(
    fsm: SseFsm,
    currentCheck: SseMessageCheck,
    remainingChecks: List[SseMessageCheck],
    checkSequenceStart: Long,
    remainingCheckSequences: List[SseMessageCheckSequence],
    session: Session,
    next: Action
) extends SseState(fsm)
    with StrictLogging {

  import fsm._

  override def onTimeout(): NextSseState = {
    logger.debug(s"Check timeout")
    // check timeout
    // fail check, send next and goto Idle
    val errorMessage = s"Check ${currentCheck.name} timeout"
    val newSession = logResponse(session, currentCheck.name, checkSequenceStart, clock.nowMillis, KO, None, Some(errorMessage))
    logger.debug(s"$errorMessage, failing it and performing next action")

    NextSseState(new SseIdleState(fsm, newSession), () => next ! newSession)
  }

  override def onSseStreamConnected(timestamp: Long): NextSseState = {
    logger.debug("SSE Stream reconnected while in PerformingChecks state")
    NextSseState(this)
  }

  override def onSseReceived(message: String, timestamp: Long): NextSseState =
    tryApplyingChecks(message, timestamp, currentCheck.matchConditions, currentCheck.checks)

  override def onSseEndOfStream(timestamp: Long): NextSseState = {
    // unexpected end of stream, fail check
    logger.debug(s"Server notified of end of stream while in PerformingChecks state")
    cancelTimeout()
    handleSseCheckCrash(currentCheck.name, session, next, None, "End of stream")
  }

  override def onSseStreamCrashed(t: Throwable, timestamp: Long): NextSseState = {
    // crash, fail check
    logger.debug("SSE stream crashed while in PerformingChecks state", t)
    cancelTimeout()
    handleSseCheckCrash(currentCheck.name, session, next, None, t.getMessage)
  }

  private def tryApplyingChecks(message: String, timestamp: Long, matchConditions: List[SseCheck], checks: List[SseCheck]): NextSseState = {

    // cache is used for both matching and checking
    val preparedCache = Check.newPreparedCache

    // if matchConditions isEmpty, all messages are considered to be matching
    val messageMatches = matchConditions.forall {
      _.check(message, session, preparedCache) match {
        case _: Success[_] => true
        case _             => false
      }
    }
    if (messageMatches) {
      logger.debug(s"Received matching message $message")
      cancelTimeout()
      // matching message, apply checks
      val (sessionWithCheckUpdate, checkError) = Check.check(message, session, checks, preparedCache)

      checkError match {
        case Some(Failure(errorMessage)) =>
          logger.debug("Check failed, performing next action")
          val newSession = logResponse(sessionWithCheckUpdate, currentCheck.name, checkSequenceStart, timestamp, KO, None, Some(errorMessage))

          NextSseState(new SseIdleState(fsm, newSession), () => next ! newSession)

        case _ =>
          logger.debug("Current check success")
          // check success
          val newSession = logResponse(sessionWithCheckUpdate, currentCheck.name, checkSequenceStart, timestamp, OK, None, None)
          remainingChecks match {
            case nextCheck :: nextRemainingChecks =>
              // perform next check
              logger.debug("Perform next check of current check sequence")
              //[fl]
              //
              //[fl]
              NextSseState(
                this.copy(
                  currentCheck = nextCheck,
                  remainingChecks = nextRemainingChecks,
                  session = newSession
                )
              )

            case _ =>
              remainingCheckSequences match {
                case SseMessageCheckSequence(timeout, nextCheck :: nextRemainingChecks) :: nextRemainingCheckSequences =>
                  logger.debug("Perform next check sequence")
                  // perform next CheckSequence
                  scheduleTimeout(timeout)
                  //[fl]
                  //
                  //[fl]
                  NextSseState(
                    this.copy(
                      currentCheck = nextCheck,
                      remainingChecks = nextRemainingChecks,
                      checkSequenceStart = timestamp,
                      remainingCheckSequences = nextRemainingCheckSequences,
                      session = newSession
                    )
                  )

                case _ =>
                  // all check sequences complete
                  logger.debug("Check sequences completed successfully")
                  NextSseState(new SseIdleState(fsm, newSession), () => next ! newSession)
              }
          }
      }
    } else {
      logger.debug(s"Received non-matching message $message")
      // server unmatched message, just log
      logUnmatchedServerMessage(session)
      NextSseState(this)
    }
  }

  private def handleSseCheckCrash(
      checkName: String,
      session: Session,
      next: Action,
      code: Option[String],
      errorMessage: String
  ): NextSseState = {
    val fullMessage = s"SSE crashed while waiting for check: $errorMessage"

    val newSession = logResponse(session, checkName, checkSequenceStart, clock.nowMillis, KO, code, Some(fullMessage))
    logger.debug("SSE crashed, performing next action")

    NextSseState(new SseCrashedState(fsm, errorMessage), () => next ! newSession)
  }
}

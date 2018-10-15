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

import java.util.{ HashMap => JHashMap }

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.validation.{ Failure, Success }
import io.gatling.core.action.Action
import io.gatling.core.check.Check
import io.gatling.core.session.Session
import io.gatling.http.check.sse.{ SseCheck, SseMessageCheckSequence }

trait WhenPerformingCheck { this: SseActor =>

  when(PerformingCheck) {

    case Event(Timeout(timeoutId), PerformingCheckData(stream, currentCheck, _, checkSequenceStart, currentTimeoutId, _, session, next)) =>
      if (timeoutId == currentTimeoutId) {
        logger.debug(s"Check timeout $timeoutId")
        // check timeout
        // fail check, send next and goto Idle
        val errorMessage = s"Check ${currentCheck.name} timeout"
        val newSession = logResponse(session, currentCheck.name, checkSequenceStart, clock.nowMillis, KO, None, Some(errorMessage))
        val nextAction = next match {
          case Left(n) =>
            logger.debug("Check timeout, failing it and performing next action")
            n
          case Right(sendFrame) =>
            // logging crash
            logger.debug("Check timeout while trying to reconnect, failing pending send message and performing next action")
            statsEngine.logCrash(newSession, sendFrame.actionName, s"Couldn't reconnect: $errorMessage")
            sendFrame.next
        }
        nextAction ! newSession
        goto(Idle) using IdleData(newSession, stream)
      } else {
        logger.debug(s"Out-of-band timeout $timeoutId")
        // out-of-band timeoutId, ignore
        stay()
      }

    case Event(SseReceived(message, timestamp), data: PerformingCheckData) =>
      tryApplyingChecks(message, timestamp, data.currentCheck.matchConditions, data.currentCheck.checks, data)

    case Event(SseStreamClosed(_), PerformingCheckData(_, currentCheck, _, checkSequenceStart, _, _, session, next)) =>
      // unexpected close, fail check
      logger.debug("WebSocket remotely closed while waiting for checks")
      cancelTimeout()
      handleSseCheckCrash(currentCheck.name, session, next, checkSequenceStart, None, "Socket closed")

    case Event(SseStreamCrashed(t, _), PerformingCheckData(_, currentCheck, _, checkSequenceStart, _, _, session, next)) =>
      // crash, fail check
      logger.debug("WebSocket crashed while waiting for checks")
      cancelTimeout()
      handleSseCheckCrash(currentCheck.name, session, next, checkSequenceStart, None, t.getMessage)
  }

  private def handleSseCheckCrash(checkName: String, session: Session, next: Either[Action, SetCheck], checkSequenceStart: Long, code: Option[String], errorMessage: String): State = {
    val fullMessage = s"WebSocket crashed while waiting for check: $errorMessage"

    val newSession = logResponse(session, checkName, checkSequenceStart, clock.nowMillis, KO, code, Some(fullMessage))
    val nextAction = next match {
      case Left(n) =>
        // failed to connect
        logger.debug("WebSocket crashed, performing next action")
        n

      case Right(sendTextMessage) =>
        // failed to reconnect, logging crash
        logger.debug("WebSocket crashed while trying to reconnect, failing pending send message and performing next action")
        statsEngine.logCrash(newSession, sendTextMessage.actionName, s"Couldn't reconnect: $errorMessage")
        sendTextMessage.next
    }
    nextAction ! newSession
    goto(Crashed) using CrashedData(Some(errorMessage))
  }

  private def tryApplyingChecks(message: String, timestamp: Long, matchConditions: List[SseCheck], checks: List[SseCheck], data: PerformingCheckData): State = {

    import data._

    // cache is used for both matching and checking
    implicit val cache = new JHashMap[Any, Any]

    // if matchConditions isEmpty, all messages are considered to be matching
    val messageMatches = matchConditions.forall(_.check(message, session).isInstanceOf[Success[_]])
    if (messageMatches) {
      logger.debug(s"Received matching message $message")
      cancelTimeout() // note, we might already have a Timeout in the mailbox, hence the currentTimeoutId check
      // matching message, apply checks
      val (sessionWithCheckUpdate, checkError) = Check.check(message, session, checks)

      checkError match {
        case Some(Failure(errorMessage)) =>
          logger.debug("Check failure")
          val newSession = logResponse(sessionWithCheckUpdate, currentCheck.name, checkSequenceStart, timestamp, KO, None, Some(errorMessage))

          val nextAction = next match {
            case Left(n) =>
              logger.debug("Check failed, performing next action")
              n
            case Right(sendMessage) =>
              // failed to reconnect, logging crash
              logger.debug("Check failed while trying to reconnect, failing pending send message and performing next action")
              statsEngine.logCrash(newSession, sendMessage.actionName, s"Couldn't reconnect: $errorMessage")
              sendMessage.next
          }

          nextAction ! newSession
          goto(Idle) using IdleData(newSession, stream)

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
              stay() using data.copy(currentCheck = nextCheck, remainingChecks = nextRemainingChecks, session = newSession)

            case _ =>
              remainingCheckSequences match {
                case SseMessageCheckSequence(timeout, newCurrentCheck :: newRemainingChecks) :: nextRemainingCheckSequences =>
                  logger.debug("Perform next check sequence")
                  // perform next CheckSequence
                  val timeoutId = scheduleTimeout(timeout)
                  //[fl]
                  //
                  //[fl]
                  stay() using data.copy(
                    currentCheck = newCurrentCheck,
                    remainingChecks = newRemainingChecks,
                    checkSequenceStart = timestamp,
                    checkSequenceTimeoutId = timeoutId,
                    remainingCheckSequences = nextRemainingCheckSequences,
                    session = newSession
                  )

                case _ =>
                  // all check sequences complete
                  logger.debug("Check sequences completed successfully")
                  next match {
                    case Left(nextAction)   => nextAction ! newSession
                    case Right(sendMessage) => self ! sendMessage.copyWithSession(newSession)
                  }
                  goto(Idle) using IdleData(newSession, stream)
              }
          }
      }
    } else {
      logger.debug(s"Received non-matching message $message")
      // server unmatched message, just log
      logUnmatchedServerMessage(session)
      stay()
    }
  }
}

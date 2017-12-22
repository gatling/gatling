/*
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

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.commons.validation.{ Failure, Success }
import io.gatling.core.action.Action
import io.gatling.core.check.Check
import io.gatling.core.session.Session

trait WhenPerformingCheck { this: WsActor =>

  when(PerformingCheck) {

    case Event(Timeout(timeoutId), PerformingCheckData(webSocket, currentCheck, _, checkSequenceStart, currentTimeoutId, _, session, next)) =>
      if (timeoutId == currentTimeoutId) {
        logger.debug(s"Check timeout $timeoutId")
        // check timeout
        // fail check, send next and goto Idle
        val errorMessage = s"Check ${currentCheck.name} timeout"
        val newSession = logResponse(session, currentCheck.name, checkSequenceStart, nowMillis, KO, None, Some(errorMessage))
        val nextAction = next match {
          case Left(n) =>
            logger.debug("Check timeout, failing it and performing next action")
            n
          case Right(sendTextMessage) =>
            // logging crash
            logger.debug("Check timeout while trying to reconnect, failing pending send message and performing next action")
            statsEngine.logCrash(newSession, sendTextMessage.actionName, s"Couldn't reconnect: $errorMessage")
            sendTextMessage.next
        }
        nextAction ! newSession
        goto(Idle) using IdleData(newSession, webSocket)
      } else {
        logger.debug(s"Out-of-band timeout $timeoutId")
        // out-of-band timeoutId, ignore
        stay()
      }

    case Event(TextMessageReceived(message, timestamp), data @ PerformingCheckData(webSocket, currentCheck, remainingChecks, checkSequenceStart, _, remainingCheckSequences, session, next)) =>

      // cache is used for both matching and checking
      implicit val cache = collection.mutable.HashMap.empty[Any, Any]

      // if matchConditions isEmpty, all messages are considered to be matching
      val messageMatches = currentCheck.matchConditions.forall(_.check(message, session).isInstanceOf[Success[_]])

      if (messageMatches) {
        logger.debug(s"Received matching message $message")
        cancelTimeout() // note, we might already have a Timeout in the mailbox, hence the currentTimeoutId check
        // matching message, apply checks
        val (checkSaveUpdate, checkError) = Check.check(message, session, currentCheck.checks)

        val sessionWithCheckUpdate = checkSaveUpdate(session)

        checkError match {
          case Some(Failure(errorMessage)) =>
            logger.debug("Check failure")
            val newSession = logResponse(sessionWithCheckUpdate, currentCheck.name, checkSequenceStart, timestamp, KO, None, Some(errorMessage))

            val nextAction = next match {
              case Left(n) =>
                logger.debug("Check failed, performing next action")
                n
              case Right(sendTextMessage) =>
                // failed to reconnect, logging crash
                logger.debug("Check failed while trying to reconnect, failing pending send message and performing next action")
                statsEngine.logCrash(newSession, sendTextMessage.actionName, s"Couldn't reconnect: $errorMessage")
                sendTextMessage.next
            }

            nextAction ! newSession
            goto(Idle) using IdleData(newSession, webSocket)

          case _ =>
            logger.debug("Current check success")
            // check success
            val newSession = logResponse(sessionWithCheckUpdate, currentCheck.name, checkSequenceStart, timestamp, OK, None, None)
            remainingChecks match {
              case Nil =>
                remainingCheckSequences match {
                  case firstCheckSequence :: nextRemainingCheckSequences =>
                    logger.debug("Perform next check sequence")
                    // perform next CheckSequence
                    val timeoutId = scheduleTimeout(firstCheckSequence.timeout)
                    //[fl]
                    //
                    //[fl]
                    stay() using data.copy(
                      currentCheck = firstCheckSequence.head,
                      remainingChecks = firstCheckSequence.tail,
                      checkSequenceStart = timestamp,
                      checkSequenceTimeoutId = timeoutId,
                      remainingCheckSequences = nextRemainingCheckSequences,
                      session = newSession
                    )

                  case _ =>
                    // all check sequences complete
                    logger.debug("Check sequences completed successfully")
                    next match {
                      case Left(nextAction)       => nextAction ! newSession
                      case Right(sendTextMessage) => self ! sendTextMessage.copy(session = newSession)
                    }
                    goto(Idle) using IdleData(newSession, webSocket)
                }

              case nextCheck :: nextRemainingChecks =>
                // perform next check
                logger.debug("Perform next check of current check sequence")
                //[fl]
                //
                //[fl]
                stay() using data.copy(currentCheck = nextCheck, remainingChecks = nextRemainingChecks, session = newSession)
            }
        }
      } else {
        logger.debug(s"Received non-matching message $message")
        // server unmatched message, just log
        logUnmatchedServerMessage(session)
        stay()
      }

    case Event(WebSocketClosed(code, reason, _), PerformingCheckData(_, currentCheck, _, checkSequenceStart, _, _, session, next)) =>
      // unexpected close, fail check
      logger.debug("WebSocket remotely closed while waiting for checks")
      cancelTimeout()
      handleWebSocketCheckCrash(currentCheck.name, session, next, checkSequenceStart, Some(Integer.toString(code)), reason)

    case Event(WebSocketCrashed(t, _), PerformingCheckData(_, currentCheck, _, checkSequenceStart, _, _, session, next)) =>
      // crash, fail check
      logger.debug("WebSocket crashed while waiting for checks")
      cancelTimeout()
      handleWebSocketCheckCrash(currentCheck.name, session, next, checkSequenceStart, None, t.getMessage)
  }

  private def handleWebSocketCheckCrash(checkName: String, session: Session, next: Either[Action, SendTextMessage], checkSequenceStart: Long, code: Option[String], errorMessage: String): State = {
    val fullMessage = s"WebSocket crashed while waiting for check: $errorMessage"

    val newSession = logResponse(session, checkName, checkSequenceStart, nowMillis, KO, code, Some(fullMessage))
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
}

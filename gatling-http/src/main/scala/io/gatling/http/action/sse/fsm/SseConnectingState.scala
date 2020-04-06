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
import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.Throwables._
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.action.sse.SseListener
import io.gatling.http.cache.SslContextSupport
import io.gatling.http.check.sse.SseMessageCheckSequence
import io.netty.handler.codec.http.HttpResponseStatus

object SseConnectingState {

  private val SseConnectSuccessStatusCode = Some(Integer.toString(HttpResponseStatus.OK.code))

  def gotoConnecting(fsm: SseFsm, session: Session, next: Either[Action, SetCheck]): NextSseState =
    gotoConnecting(fsm, session, next, fsm.httpProtocol.wsPart.maxReconnects.getOrElse(0))

  def gotoConnecting(fsm: SseFsm, session: Session, next: Either[Action, SetCheck], remainingTries: Int): NextSseState = {

    import fsm._

    val listener = new SseListener(fsm, statsEngine, clock)

    // [fl]
    //
    // [fl]
    val userSslContexts = SslContextSupport.sslContexts(session)
    httpEngine.executeRequest(
      connectRequest,
      session.userId,
      httpProtocol.enginePart.shareConnections,
      session.eventLoop,
      listener,
      userSslContexts.map(_.sslContext).orNull,
      userSslContexts.flatMap(_.alpnSslContext).orNull
    )

    NextSseState(new SseConnectingState(fsm, session, next, clock.nowMillis, remainingTries))
  }
}

class SseConnectingState(fsm: SseFsm, session: Session, next: Either[Action, SetCheck], connectStart: Long, remainingTries: Int)
    extends SseState(fsm)
    with StrictLogging {

  import fsm._

  private def handleConnectFailure(
      session: Session,
      next: Either[Action, SetCheck],
      connectStart: Long,
      connectEnd: Long,
      code: Option[String],
      reason: String,
      remainingTries: Int
  ): NextSseState = {
    // log connect failure
    val newSession = logResponse(session, connectActionName, connectStart, connectEnd, KO, code, Some(reason))
    val newRemainingTries = remainingTries - 1
    if (newRemainingTries > 0) {
      // try again
      logger.debug(s"Connect failed: $code:$reason, retrying ($newRemainingTries remaining tries)")
      SseConnectingState.gotoConnecting(fsm, newSession, next, newRemainingTries)

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

      NextSseState(
        new SseCrashedState(fsm, Some(reason)),
        () => nextAction ! newSession.markAsFailed
      )
    }
  }

  override def onSseStreamConnected(stream: SseStream, connectEnd: Long): NextSseState = {
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
            stream = stream,
            currentCheck = currentCheck,
            remainingChecks = remainingChecks,
            checkSequenceStart = connectEnd,
            remainingCheckSequences = remainingCheckSequences,
            session = sessionWithGroupTimings,
            next = next
          )
        )

      case _ => // same as Nil as WsFrameCheckSequence#checks can't be Nil, but compiler complains that match may not be exhaustive
        // send next
        next match {
          case Left(nextAction) =>
            logger.debug("Connected, no checks, performing next action")
            nextAction ! sessionWithGroupTimings

          case Right(setCheck) =>
            logger.debug("Reconnected, no checks, sending pending message")
            setCheckNextAction(sessionWithGroupTimings, setCheck)
        }
        NextSseState(new SseIdleState(fsm, sessionWithGroupTimings, stream))

    }
  }
  override def onSseStreamClosed(timestamp: Long): NextSseState =
    // unexpected close
    handleConnectFailure(session, next, connectStart, timestamp, None, "Socket closed", remainingTries)

  override def onSseStreamCrashed(t: Throwable, timestamp: Long): NextSseState =
    // crash
    handleConnectFailure(session, next, connectStart, timestamp, None, t.rootMessage, remainingTries)
}

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

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.Throwables._
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.check.sse._

import com.typesafe.scalalogging.StrictLogging

object NextSseState {
  val DoNothing: () => Unit = () => {}
}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class NextSseState(state: SseState, afterStateUpdate: () => Unit = NextSseState.DoNothing)

abstract class SseState(fsm: SseFsm) extends StrictLogging {

  private val stateName = getClass.getSimpleName

  def onSseStreamConnected(timestamp: Long): NextSseState =
    onIllegalState(s"Can't call onSseStreamConnected in $stateName state", timestamp)

  def onSetCheck(actionName: String, checkSequences: List[SseMessageCheckSequence], session: Session, next: Action): NextSseState =
    onIllegalState(s"Can't call onSetCheck in $stateName state", fsm.clock.nowMillis)

  def onSseReceived(message: String, timestamp: Long): NextSseState =
    onIllegalState(s"Can't call onSseReceived in $stateName state", timestamp)

  def onSseEndOfStream(timestamp: Long): NextSseState =
    onIllegalState(s"Can't call onSseEndOfStream in $stateName state", timestamp)

  def onSseStreamClosed(timestamp: Long): NextSseState =
    onIllegalState(s"Can't call onSseStreamClosed in $stateName state", timestamp)

  def onClientCloseRequest(actionName: String, session: Session, next: Action): NextSseState =
    onIllegalState(s"Can't call onClientCloseRequest in $stateName state", fsm.clock.nowMillis)

  def onTimeout(): NextSseState =
    onIllegalState(s"Can't call onTimeout in $stateName state", fsm.clock.nowMillis)

  def onSseStreamCrashed(t: Throwable, timestamp: Long): NextSseState = {
    logger.debug(s"WebSocket crashed by the server while in $stateName state", t)
    NextSseState(new SseCrashedState(fsm, t.rootMessage))
  }

  private def onIllegalState(message: String, timestamp: Long): NextSseState = {
    val error = new IllegalStateException(message)
    logger.error(error.getMessage, error)
    onSseStreamCrashed(error, timestamp)
  }

  protected def logUnmatchedServerMessage(session: Session): Unit =
    fsm.statsEngine.logResponse(session.scenario, session.groups, fsm.sseName, fsm.clock.nowMillis, Long.MinValue, OK, None, None)

  protected def logResponse(
      session: Session,
      actionName: String,
      start: Long,
      end: Long,
      status: Status,
      code: Option[String],
      reason: Option[String]
  ): Session = {
    val newSession = session.logGroupRequestTimings(start, end)
    val newSessionWithMark = if (status == KO) newSession.markAsFailed else newSession
    fsm.statsEngine.logResponse(session.scenario, session.groups, actionName, start, end, status, code, reason)
    newSessionWithMark
  }

  //[fl]
  //
  //
  //[fl]

  protected def setCheckNextAction(session: Session, setCheck: SetCheck): () => Unit =
    () => fsm.onSetCheck(setCheck.actionName, setCheck.checkSequences, session, setCheck.next)
}

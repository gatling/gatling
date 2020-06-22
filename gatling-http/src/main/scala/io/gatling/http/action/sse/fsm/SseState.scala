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

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.check.sse._

object NextSseState {
  val DoNothing: () => Unit = () => {}
}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class NextSseState(state: SseState, afterStateUpdate: () => Unit = NextSseState.DoNothing)

abstract class SseState(fsm: SseFsm) {

  import fsm._

  def onSseStreamConnected(timestamp: Long): NextSseState =
    throw new IllegalStateException(s"Can't call onSseStreamConnected in ${getClass.getSimpleName} state")
  def onSetCheck(actionName: String, checkSequences: List[SseMessageCheckSequence], session: Session, next: Action): NextSseState =
    throw new IllegalStateException(s"Can't call onSetCheck in ${getClass.getSimpleName} state")
  def onSseReceived(message: String, timestamp: Long): NextSseState =
    throw new IllegalStateException(s"Can't call onSseReceived in ${getClass.getSimpleName} state")
  def onSseEndOfStream(timestamp: Long): NextSseState =
    throw new IllegalStateException(s"Can't call onSseEndOfStream in ${getClass.getSimpleName} state")
  def onSseStreamClosed(timestamp: Long): NextSseState =
    throw new IllegalStateException(s"Can't call onSseStreamClosed in ${getClass.getSimpleName} state")
  def onSseStreamCrashed(t: Throwable, timestamp: Long): NextSseState =
    throw new IllegalStateException(s"Can't call onSseStreamCrashed in ${getClass.getSimpleName} state")
  def onClientCloseRequest(actionName: String, session: Session, next: Action): NextSseState =
    throw new IllegalStateException(s"Can't call onClientCloseRequest in ${getClass.getSimpleName} state")
  def onTimeout(): NextSseState = throw new IllegalStateException(s"Can't call onTimeout in ${getClass.getSimpleName} state")

  protected def logUnmatchedServerMessage(session: Session): Unit =
    statsEngine.logResponse(session.scenario, session.groups, sseName, clock.nowMillis, Long.MinValue, OK, None, None)

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
    statsEngine.logResponse(session.scenario, session.groups, actionName, start, end, status, code, reason)
    newSessionWithMark
  }

  //[fl]
  //
  //
  //[fl]

  protected def setCheckNextAction(session: Session, setCheck: SetCheck): () => Unit =
    () => fsm.onSetCheck(setCheck.actionName, setCheck.checkSequences, session, setCheck.next)
}

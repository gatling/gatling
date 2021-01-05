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

package io.gatling.http.action.ws.fsm

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.Throwables._
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.check.ws._
import io.gatling.http.client.WebSocket

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.cookie.Cookie

object NextWsState {
  val DoNothing: () => Unit = () => {}
}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class NextWsState(state: WsState, afterStateUpdate: () => Unit = NextWsState.DoNothing)

abstract class WsState(fsm: WsFsm) extends StrictLogging {

  private val stateName = getClass.getSimpleName

  def onPerformInitialConnect(session: Session, initialConnectNext: Action): NextWsState =
    onIllegalState(s"Can't call onPerformInitialConnect in $stateName state", fsm.clock.nowMillis)

  def onWebSocketConnected(webSocket: WebSocket, cookies: List[Cookie], timestamp: Long): NextWsState =
    onIllegalState(s"Can't call onWebSocketConnected in $stateName state", timestamp)

  def onSendTextFrame(
      actionName: String,
      message: String,
      checkSequences: List[WsFrameCheckSequence[WsTextFrameCheck]],
      session: Session,
      next: Action
  ): NextWsState =
    onIllegalState(s"Can't call onSendTextFrame in $stateName state", fsm.clock.nowMillis)

  def onSendBinaryFrame(
      actionName: String,
      message: Array[Byte],
      checkSequences: List[WsFrameCheckSequence[WsBinaryFrameCheck]],
      session: Session,
      next: Action
  ): NextWsState =
    onIllegalState(s"Unexpected onSendBinaryFrame in $stateName state", fsm.clock.nowMillis)

  def onTextFrameReceived(message: String, timestamp: Long): NextWsState =
    onIllegalState(s"Unexpected onTextFrameReceived in $stateName state", timestamp)

  def onBinaryFrameReceived(message: Array[Byte], timestamp: Long): NextWsState =
    onIllegalState(s"Unexpected onBinaryFrameReceived in $stateName state", timestamp)

  def onWebSocketClosed(code: Int, reason: String, timestamp: Long): NextWsState =
    onIllegalState(s"Unexpected onWebSocketClosed in $stateName state", timestamp)

  def onClientCloseRequest(actionName: String, session: Session, next: Action): NextWsState =
    onIllegalState(s"Unexpected onClientCloseRequest call in $stateName state", fsm.clock.nowMillis)

  def onTimeout(): NextWsState =
    onIllegalState(s"Unexpected onTimeout call in $stateName state", fsm.clock.nowMillis)

  private def onIllegalState(message: String, timestamp: Long): NextWsState = {
    val error = new IllegalStateException(message)
    logger.error(error.getMessage, error)
    onWebSocketCrashed(error, timestamp)
  }

  def onWebSocketCrashed(t: Throwable, timestamp: Long): NextWsState = {
    logger.debug(s"WebSocket crashed by the server while in $stateName state", t)
    NextWsState(new WsCrashedState(fsm, Some(t.rootMessage)))
  }

  //[fl]
  //
  //
  //[fl]

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

  protected def logUnmatchedServerMessage(session: Session): Unit =
    fsm.statsEngine.logResponse(session.scenario, session.groups, fsm.wsName, fsm.clock.nowMillis, Long.MinValue, OK, None, None)

  protected def sendFrameNextAction(session: Session, sendFrame: SendFrame): () => Unit =
    sendFrame match {
      case SendTextFrame(actionName, message, checkSequences, next) =>
        () => fsm.onSendTextFrame(actionName, message, checkSequences, session, next)
      case SendBinaryFrame(actionName, message, checkSequences, next) =>
        () => fsm.onSendBinaryFrame(actionName, message, checkSequences, session, next)
    }
}

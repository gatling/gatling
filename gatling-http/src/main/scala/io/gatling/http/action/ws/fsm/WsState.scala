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

package io.gatling.http.action.ws.fsm

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.check.ws.{ WsBinaryFrameCheck, WsFrameCheck, WsFrameCheckSequence, WsTextFrameCheck }
import io.gatling.http.client.WebSocket
import io.netty.handler.codec.http.cookie.Cookie

object NextWsState {
  val DoNothing: () => Unit = () => {}
}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class NextWsState(state: WsState, afterStateUpdate: () => Unit = NextWsState.DoNothing)

abstract class WsState(fsm: WsFsm) {

  def onPerformInitialConnect(session: Session, initialConnectNext: Action): NextWsState =
    throw new IllegalStateException(s"Can't call onPerformInitialConnect in ${getClass.getSimpleName} state")

  def onWebSocketConnected(webSocket: WebSocket, cookies: List[Cookie], timestamp: Long): NextWsState =
    throw new IllegalStateException(s"Can't call onWebSocketConnected in ${getClass.getSimpleName} state")

  def onSendTextFrame(
      actionName: String,
      message: String,
      checkSequences: List[WsFrameCheckSequence[WsTextFrameCheck]],
      session: Session,
      next: Action
  ): NextWsState =
    throw new IllegalStateException(s"Can't call onSendTextFrame in ${getClass.getSimpleName} state")

  def onSendBinaryFrame(
      actionName: String,
      message: Array[Byte],
      checkSequences: List[WsFrameCheckSequence[WsBinaryFrameCheck]],
      session: Session,
      next: Action
  ): NextWsState =
    throw new IllegalStateException(s"Can't call onSendBinaryFrame in ${getClass.getSimpleName} state")

  def onTextFrameReceived(message: String, timestamp: Long): NextWsState =
    throw new IllegalStateException(s"Can't call onTextFrameReceived in ${getClass.getSimpleName} state")

  def onBinaryFrameReceived(message: Array[Byte], timestamp: Long): NextWsState =
    throw new IllegalStateException(s"Can't call onBinaryFrameReceived in ${getClass.getSimpleName} state")

  def onWebSocketClosed(code: Int, reason: String, timestamp: Long): NextWsState =
    throw new IllegalStateException(s"Can't call onWebSocketClosed in ${getClass.getSimpleName} state")

  def onWebSocketCrashed(t: Throwable, timestamp: Long): NextWsState =
    throw new IllegalStateException(s"Can't call onWebSocketCrashed in ${getClass.getSimpleName} state")

  def onClientCloseRequest(actionName: String, session: Session, next: Action): NextWsState =
    throw new IllegalStateException(s"Can't call onClientCloseRequest in ${getClass.getSimpleName} state")

  def onTimeout(): NextWsState =
    throw new IllegalStateException(s"Can't call onTimeout in ${getClass.getSimpleName} state")

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
    fsm.statsEngine.logResponse(newSessionWithMark, actionName, start, end, status, code, reason)
    newSessionWithMark
  }

  protected def logUnmatchedServerMessage(session: Session): Unit =
    fsm.statsEngine.logResponse(session, fsm.wsName, fsm.clock.nowMillis, Long.MinValue, OK, None, None)

  protected def sendFrameNextAction(session: Session, sendFrame: SendFrame): () => Unit =
    sendFrame match {
      case SendTextFrame(actionName, message, checkSequences, next) =>
        () => fsm.onSendTextFrame(actionName, message, checkSequences, session, next)
      case SendBinaryFrame(actionName, message, checkSequences, next) =>
        () => fsm.onSendBinaryFrame(actionName, message, checkSequences, session, next)
    }
}

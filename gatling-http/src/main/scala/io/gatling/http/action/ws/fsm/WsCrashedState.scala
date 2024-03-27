/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.stats.KO
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.check.ws.{ WsFrameCheck, WsFrameCheckSequence }

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus

final class WsCrashedState(fsm: WsFsm, errorMessage: Option[String], val remainingReconnects: Int) extends WsState(fsm) with StrictLogging {
  override def onClientCloseRequest(actionName: String, closeStatus: WebSocketCloseStatus, session: Session, next: Action): NextWsState = {
    val newSession = (errorMessage match {
      case Some(mess) =>
        val newSession = session.markAsFailed
        fsm.statsEngine.logRequestCrash(session.scenario, session.groups, actionName, s"Client issued close order but WebSocket was already crashed: $mess")
        newSession
      case _ =>
        logger.debug("Client issued close order but WebSocket was already closed")
        session
    }).remove(fsm.wsName)

    NextWsState(new WsClosedState(fsm), () => next ! newSession)
  }

  override def onSendTextFrame(
      actionName: String,
      message: String,
      checkSequences: List[WsFrameCheckSequence[WsFrameCheck]],
      session: Session,
      next: Action
  ): NextWsState =
    handleSendFrameFailure(
      actionName,
      session,
      next,
      SendTextFrame(actionName, message, checkSequences, next),
      "text"
    )

  override def onSendBinaryFrame(
      actionName: String,
      message: Array[Byte],
      checkSequences: List[WsFrameCheckSequence[WsFrameCheck]],
      session: Session,
      next: Action
  ): NextWsState =
    handleSendFrameFailure(
      actionName,
      session,
      next,
      SendBinaryFrame(actionName, message, checkSequences, next),
      "binary"
    )

  private def handleSendFrameFailure(
      actionName: String,
      session: Session,
      next: Action,
      afterReconnectAction: SendFrame,
      frameType: String
  ): NextWsState = {
    val loggedMessage = errorMessage match {
      case Some(mess) => s"Client issued a $frameType frame but WebSocket was already crashed: $mess"
      case _          => s"Client issued a $frameType frame but WebSocket was already closed"
    }

    logger.debug(loggedMessage)

    if (remainingReconnects > 0) {
      logger.debug(s"Reconnecting WebSocket remainingReconnects=$remainingReconnects")
      // perform blocking reconnect
      WsConnectingState.gotoConnecting(fsm, session, Right(afterReconnectAction), remainingReconnects - 1)
    } else {
      val now = fsm.clock.nowMillis
      val message = s"Client issued $frameType frame but server has closed the WebSocket and max reconnects is reached"
      logger.debug(message)
      val newSession = logResponse(
        session,
        actionName,
        now,
        now,
        KO,
        None,
        Some(message)
      )

      NextWsState(this, () => next ! newSession)
    }
  }
}

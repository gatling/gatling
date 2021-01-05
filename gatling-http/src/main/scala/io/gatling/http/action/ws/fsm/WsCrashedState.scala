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

import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.check.ws.{ WsBinaryFrameCheck, WsFrameCheckSequence, WsTextFrameCheck }

import com.typesafe.scalalogging.StrictLogging

final class WsCrashedState(fsm: WsFsm, errorMessage: Option[String]) extends WsState(fsm) with StrictLogging {

  override def onClientCloseRequest(actionName: String, session: Session, next: Action): NextWsState = {
    val newSession = (errorMessage match {
      case Some(mess) =>
        val newSession = session.markAsFailed
        fsm.statsEngine.logCrash(session.scenario, session.groups, actionName, s"Client issued close order but WebSocket was already crashed: $mess")
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
      checkSequences: List[WsFrameCheckSequence[WsTextFrameCheck]],
      session: Session,
      next: Action
  ): NextWsState = {
    // FIXME sent message so be stashed until reconnect, instead of failed
    val loggedMessage = errorMessage match {
      case Some(mess) => s"Client issued message but WebSocket was already crashed: $mess"
      case _          => "Client issued message but WebSocket was already closed"
    }

    logger.debug(loggedMessage)

    // perform blocking reconnect
    WsConnectingState.gotoConnecting(fsm, session, Right(SendTextFrame(actionName, message, checkSequences, next)))
  }

  override def onSendBinaryFrame(
      actionName: String,
      message: Array[Byte],
      checkSequences: List[WsFrameCheckSequence[WsBinaryFrameCheck]],
      session: Session,
      next: Action
  ): NextWsState = {
    // FIXME sent message so be stashed until reconnect, instead of failed
    val loggedMessage = errorMessage match {
      case Some(mess) => s"Client issued message but WebSocket was already crashed: $mess"
      case _          => "Client issued message but WebSocket was already closed"
    }

    logger.debug(loggedMessage)

    // perform blocking reconnect
    WsConnectingState.gotoConnecting(fsm, session, Right(SendBinaryFrame(actionName, message, checkSequences, next)))
  }
}

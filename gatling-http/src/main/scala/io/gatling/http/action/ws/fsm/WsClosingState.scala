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

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.core.action.Action
import io.gatling.core.session.Session

import com.typesafe.scalalogging.StrictLogging

final class WsClosingState(fsm: WsFsm, actionName: String, session: Session, next: Action, closeStart: Long) extends WsState(fsm) with StrictLogging {

  override def onTextFrameReceived(message: String, timestamp: Long): WsState = {
    logUnmatchedServerMessage(session)
    this
  }

  override def onBinaryFrameReceived(message: Array[Byte], timestamp: Long): WsState = {
    logUnmatchedServerMessage(session)
    this
  }

  override def onWebSocketClosed(code: Int, reason: String, closeEnd: Long): WsState = {
    // server has acked closing
    logger.info("Server has acked closing")
    val newSession = logResponse(session, actionName, closeStart, closeEnd, OK, None, None)
    next ! newSession.remove(fsm.wsName)
    new WsClosedState(fsm)
  }

  override def onWebSocketCrashed(t: Throwable, timestamp: Long): WsState = {
    logger.info("WebSocket crashed while waiting for close ack")
    // crash, close anyway
    val newSession = logResponse(session, actionName, closeStart, timestamp, KO, None, Some(t.getMessage))
    next ! newSession.markAsFailed.remove(fsm.wsName)
    new WsClosedState(fsm)
  }
}

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

import java.util.concurrent.{ ScheduledFuture, TimeUnit }

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.util.Clock
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.http.check.ws.{ WsBinaryFrameCheck, WsFrameCheck, WsFrameCheckSequence, WsTextFrameCheck }
import io.gatling.http.client.{ Request, WebSocket }
import io.gatling.http.engine.HttpEngine
import io.gatling.http.protocol.HttpProtocol

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.EventLoop
import io.netty.handler.codec.http.cookie.Cookie

class WsFsm(
    private[fsm] val wsName: String,
    private[fsm] val connectRequest: Request,
    private[fsm] val connectActionName: String,
    private[fsm] val connectCheckSequence: List[WsFrameCheckSequence[WsFrameCheck]],
    private[fsm] val onConnected: Option[Action],
    private[fsm] val statsEngine: StatsEngine,
    private[fsm] val httpEngine: HttpEngine,
    private[fsm] val httpProtocol: HttpProtocol,
    eventLoop: EventLoop,
    private[fsm] val clock: Clock
) extends StrictLogging {

  private var currentState: WsState = new WsInitState(this)
  private var currentTimeout: ScheduledFuture[Unit] = _
  private[fsm] def scheduleTimeout(dur: FiniteDuration): Unit = {
    currentTimeout = eventLoop.schedule(
      () => {
        logger.debug(s"Timeout ${currentTimeout.hashCode} triggered")
        currentTimeout = null
        execute(currentState.onTimeout())
      },
      dur.toMillis,
      TimeUnit.MILLISECONDS
    )
    logger.debug(s"Timeout ${currentTimeout.hashCode} scheduled")
  }

  private[fsm] def cancelTimeout(): Unit =
    if (currentTimeout == null) {
      logger.debug("Couldn't cancel timeout because it wasn't set")
    } else {
      if (currentTimeout.cancel(true)) {
        logger.debug(s"Timeout ${currentTimeout.hashCode} cancelled")
      } else {
        logger.debug(s"Failed to cancel timeout ${currentTimeout.hashCode}")
      }
      currentTimeout = null
    }

  private def execute(f: => NextWsState): Unit = {
    val NextWsState(nextState, afterStateUpdate) = f
    currentState = nextState
    afterStateUpdate()
  }

  def onPerformInitialConnect(session: Session, initialConnectNext: Action): Unit =
    execute(currentState.onPerformInitialConnect(session, initialConnectNext))

  def onWebSocketConnected(webSocket: WebSocket, cookies: List[Cookie], timestamp: Long): Unit =
    execute(currentState.onWebSocketConnected(webSocket, cookies, timestamp))

  def onSendTextFrame(
      actionName: String,
      message: String,
      checkSequences: List[WsFrameCheckSequence[WsTextFrameCheck]],
      session: Session,
      next: Action
  ): Unit =
    execute(currentState.onSendTextFrame(actionName, message, checkSequences, session, next))

  def onSendBinaryFrame(
      actionName: String,
      message: Array[Byte],
      checkSequences: List[WsFrameCheckSequence[WsBinaryFrameCheck]],
      session: Session,
      next: Action
  ): Unit =
    execute(currentState.onSendBinaryFrame(actionName, message, checkSequences, session, next))

  def onTextFrameReceived(message: String, timestamp: Long): Unit =
    execute(currentState.onTextFrameReceived(message, timestamp))

  def onBinaryFrameReceived(message: Array[Byte], timestamp: Long): Unit =
    execute(currentState.onBinaryFrameReceived(message, timestamp))

  def onWebSocketClosed(code: Int, reason: String, timestamp: Long): Unit =
    execute(currentState.onWebSocketClosed(code, reason, timestamp))

  def onWebSocketCrashed(t: Throwable, timestamp: Long): Unit =
    execute(currentState.onWebSocketCrashed(t, timestamp))

  def onClientCloseRequest(actionName: String, session: Session, next: Action): Unit =
    execute(currentState.onClientCloseRequest(actionName, session, next))
}

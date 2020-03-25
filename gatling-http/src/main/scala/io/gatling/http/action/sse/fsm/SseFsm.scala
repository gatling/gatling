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

import java.util.concurrent.{ ScheduledFuture, TimeUnit }

import io.gatling.commons.util.Clock
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.http.check.sse.SseMessageCheckSequence
import io.gatling.http.client.Request
import io.gatling.http.engine.HttpEngine
import io.gatling.http.protocol.HttpProtocol
import io.netty.channel.EventLoop

import scala.concurrent.duration.FiniteDuration

class SseFsm(
    private[fsm] val wsName: String,
    private[fsm] val connectRequest: Request,
    private[fsm] val connectActionName: String,
    private[fsm] val connectCheckSequence: List[SseMessageCheckSequence],
    private[fsm] val statsEngine: StatsEngine,
    private[fsm] val httpEngine: HttpEngine,
    private[fsm] val httpProtocol: HttpProtocol,
    private[fsm] val eventLoop: EventLoop,
    private[fsm] val clock: Clock
) {
  private var currentState: SseState = new SseInitState(this)
  private var currentTimeout: ScheduledFuture[Unit] = _
  private[fsm] def scheduleTimeout(dur: FiniteDuration): Unit =
    eventLoop.schedule(new Runnable {
      override def run(): Unit = {
        currentTimeout = null
        execute(currentState.onTimeout())
      }
    }, dur.toMillis, TimeUnit.MILLISECONDS)

  private[fsm] def cancelTimeout(): Unit =
    if (currentTimeout != null) {
      currentTimeout.cancel(true)
      currentTimeout = null
    }

  private def execute(f: => NextSseState): Unit = {
    val NextSseState(nextState, afterStateUpdate) = f
    currentState = nextState
    afterStateUpdate()
  }

  def onPerformInitialConnect(session: Session, initialConnectNext: Action): Unit =
    execute(currentState.onPerformInitialConnect(session, initialConnectNext))

  def onSseStreamConnected(stream: SseStream, timestamp: Long): Unit =
    execute(currentState.onSseStreamConnected(stream, timestamp))

  def onSetCheck(actionName: String, checkSequences: List[SseMessageCheckSequence], session: Session, next: Action): Unit =
    execute(currentState.onSetCheck(actionName, checkSequences, session: Session, next))

  def onSseReceived(message: String, timestamp: Long): Unit =
    execute(currentState.onSseReceived(message, timestamp))

  def onSseStreamClosed(timestamp: Long): Unit =
    execute(currentState.onSseStreamClosed(timestamp))

  def onSseStreamCrashed(t: Throwable, timestamp: Long): Unit =
    execute(currentState.onSseStreamCrashed(t, timestamp))

  def onClientCloseRequest(actionName: String, session: Session, next: Action): Unit =
    execute(currentState.onClientCloseRequest(actionName, session, next))
}

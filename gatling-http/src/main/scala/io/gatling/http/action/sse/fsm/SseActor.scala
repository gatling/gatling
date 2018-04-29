/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.Clock
import io.gatling.core.action.Action
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.http.check.sse.{ SseMessageCheck, SseMessageCheckSequence }
import io.gatling.http.client.Request
import io.gatling.http.engine.HttpEngine
import io.gatling.http.protocol.HttpProtocol

import akka.actor.Props

case class PerformInitialConnect(session: Session, initialConnectNext: Action)
case class SseStreamConnected(stream: SseStream, timestamp: Long)
case class SetCheck(actionName: String, checkSequences: List[SseMessageCheckSequence], session: Session, next: Action) {
  def copyWithSession(newSession: Session): SetCheck = copy(session = newSession)
}
case class SseReceived(message: String, timestamp: Long)
case class SseStreamClosed(code: Int, reason: String, timestamp: Long)
case class SseStreamCrashed(t: Throwable, timestamp: Long)
case class ClientCloseRequest(actionName: String, session: Session, next: Action)
case class Timeout(id: Long)

object SseActor {

  def props(
    sseName:              String,
    connectRequest:       Request,
    connectActionName:    String,
    connectCheckSequence: List[SseMessageCheckSequence],
    statsEngine:          StatsEngine,
    httpEngine:           HttpEngine,
    httpProtocol:         HttpProtocol,
    clock:                Clock,
    configuration:        GatlingConfiguration
  ) =
    Props(new SseActor(
      sseName,
      connectRequest,
      connectActionName,
      connectCheckSequence,
      statsEngine,
      httpEngine,
      httpProtocol,
      clock,
      configuration
    ))

  val TimeoutTimerName = "timeout"
}

class SseActor(
    val wsName:               String,
    val connectRequest:       Request,
    val connectActionName:    String,
    val connectCheckSequence: List[SseMessageCheckSequence],
    val statsEngine:          StatsEngine,
    val httpEngine:           HttpEngine,
    val httpProtocol:         HttpProtocol,
    val clock:                Clock,
    val configuration:        GatlingConfiguration
) extends SseActorFSM
  with WhenInit
  with WhenConnecting
  with WhenPerformingCheck
  with WhenIdle
  with WhenClosing
  with WhenCrashed {

  private var _timeoutId = 0L
  protected def scheduleTimeout(dur: FiniteDuration): Long = {
    val curr = _timeoutId
    setTimer(SseActor.TimeoutTimerName, Timeout(curr), dur, repeat = false)
    _timeoutId += 1
    curr
  }

  protected def cancelTimeout(): Unit =
    cancelTimer(SseActor.TimeoutTimerName)

  //[fl]
  //
  //
  //[fl]

  protected def logResponse(session: Session, actionName: String, start: Long, end: Long, status: Status, code: Option[String], reason: Option[String]): Session = {
    val newSession = session.logGroupRequest(start, end, status)
    val newSessionWithMark = if (status == KO) newSession.markAsFailed else newSession
    statsEngine.logResponse(newSessionWithMark, actionName, start, end, status, code, reason)
    newSessionWithMark
  }

  protected def logUnmatchedServerMessage(session: Session): Unit =
    statsEngine.logResponse(session, wsName, clock.nowMillis, Long.MinValue, OK, None, None)

  startWith(Init, InitData)

  override def unhandled(message: Any): Unit =
    logger.debug(s"Received unhandled message $message")

  initialize()
}

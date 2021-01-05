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

import java.util.concurrent.TimeUnit

import io.gatling.commons.util.Clock
import io.gatling.commons.util.Throwables._
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.http.action.sse.SseListener
import io.gatling.http.client.Request
import io.gatling.http.engine.HttpEngine
import io.gatling.http.util.SslContexts

import com.typesafe.scalalogging.StrictLogging

sealed trait SseStreamState
final case class Connecting(listener: SseListener) extends SseStreamState
final case class Open(listener: SseListener) extends SseStreamState
final case class ProcessingClientCloseRequest(listener: SseListener) extends SseStreamState
case object Close extends SseStreamState

object SseStream {
  private val DefaultRetryDelayInSeconds = 3
}

class SseStream(
    originalSession: Session,
    connectRequest: Request,
    connectActionName: String,
    userSslContexts: Option[SslContexts],
    shareConnections: Boolean,
    httpEngine: HttpEngine,
    statsEngine: StatsEngine,
    clock: Clock
) extends StrictLogging {

  private val groups = originalSession.groups
  private[fsm] var fsm: SseFsm = _
  private var state: SseStreamState = _
  private var retryDelayInSeconds = SseStream.DefaultRetryDelayInSeconds

  def connect(): Unit = {
    logger.debug("(re-)connecting stream")
    val listener = new SseListener(this)
    state = Connecting(listener)

    // [fl]
    //
    // [fl]
    httpEngine.executeRequest(
      connectRequest,
      originalSession.userId,
      shareConnections,
      originalSession.eventLoop,
      listener,
      userSslContexts.map(_.sslContext).orNull,
      userSslContexts.flatMap(_.alpnSslContext).orNull
    )
  }

  def connected(): Unit =
    state match {
      case Connecting(listener) =>
        logger.debug("Stream connected while in state Connecting. Processing.")
        state = Open(listener)
        fsm.onSseStreamConnected()
      case Open(listener) =>
        illegalState(listener, "Invalid state: stream was connected while state was Open. Please report.")
      case ProcessingClientCloseRequest(listener) =>
        logger.debug("Stream connected while in state ProcessingClientCloseRequest. Closing.")
        listener.closeChannel()
        fsm.onSseStreamClosed()
        state = Close
      case _ =>
        illegalState(null, "Invalid state: stream was connected while state was Close. Please report.")
    }

  def closedByServer(): Unit =
    state match {
      case Connecting(listener) =>
        illegalState(listener, "Invalid state: server closed the stream while state was Connecting. Please report.")
      case Open(_) =>
        logger.debug("Server closed the stream while in state Open. Reconnecting.")
        // reconnect
        originalSession.eventLoop.schedule(
          (() => connect()): Runnable,
          retryDelayInSeconds,
          TimeUnit.SECONDS
        )
      case ProcessingClientCloseRequest(_) =>
        logger.debug("Server closed the stream while in state ProcessingClientCloseRequest.")
        state = Close
      case _ =>
        logger.debug("Server closed the stream while in state Close.")
    }

  def endOfStream(): Unit =
    state match {
      case Connecting(listener) =>
        illegalState(listener, "Invalid state: server notified of end of stream while state was Connecting. Please report.")
      case Open(_) =>
        // don't reconnect
        state = Close
        fsm.onSseStreamClosed()
      case ProcessingClientCloseRequest(_) =>
        state = Close // so everything gets garbage collected
      case _ => // already closed, do nothing
        logger.debug("End of stream reached while in state Close.")
    }

  def requestingCloseByClient(): Unit =
    state match {
      case Connecting(listener) =>
        listener.closeChannel()
        state = ProcessingClientCloseRequest(listener)
        fsm.onSseStreamClosed()
      case Open(listener) =>
        listener.closeChannel()
        state = ProcessingClientCloseRequest(listener)
        fsm.onSseStreamClosed()
      case _ => // already closed, do nothing
    }

  def crash(throwable: Throwable): Unit = {
    if (logger.underlying.isDebugEnabled) {
      logger.debug("Sse stream crashed", throwable)
    } else {
      val errorMessage = throwable.rootMessage
      logger.debug(s"Sse stream crashed: $errorMessage")
    }

    state match {
      case Open(_) =>
        state = Close
        fsm.onSseStreamCrashed(throwable)
      case Connecting(_) =>
        state = Close
        fsm.onSseStreamCrashed(throwable)
      case ProcessingClientCloseRequest(_) => state = Close
      case _                               => // weird but ignore
    }
  }

  def eventReceived(event: ServerSentEvent): Unit =
    state match {
      case Open(_) =>
        logger.debug(s"Received SSE event $event while in Open state. Propagating.")
        event.retry.foreach(retryDelayInSeconds = _)
        fsm.onSseReceived(event.asJsonString)
      case Connecting(listener) =>
        illegalState(listener, s"Invalid state: received SSE $event while state was Connecting. Please report.")
      case ProcessingClientCloseRequest(_) =>
        logger.debug(s"Received SSE event $event while in ProcessingClientCloseRequest state. Ignoring.")
      case _ =>
        illegalState(null, s"Invalid state: received SSE $event while state was Close. Please report.")
    }

  private def illegalState(listener: SseListener, message: String): Unit = {
    fsm.onSseStreamCrashed(new IllegalStateException(message))
    if (listener != null) {
      listener.closeChannel()
    }
    state = Close
  }
}

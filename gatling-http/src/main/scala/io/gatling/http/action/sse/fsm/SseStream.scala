/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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
import io.gatling.http.client.impl.PrematureCloseException
import io.gatling.http.engine.HttpEngine
import io.gatling.http.util.SslContexts

import com.typesafe.scalalogging.StrictLogging
import io.netty.util.AsciiString

sealed trait SseStreamState
object SseStreamState {
  final case class Connecting(listener: SseListener) extends SseStreamState
  final case class Connected(listener: SseListener) extends SseStreamState
  final case class Closing(listener: SseListener) extends SseStreamState
  case object Closed extends SseStreamState
}

object SseStream {
  private val LastEventIdHeaderName = new AsciiString("Last-Event-ID")
  private val DefaultRetryDelayInMillis = 3000
}

final class SseStream(
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
  private var lastEventId: Option[String] = None
  private var retryDelayInMillis = SseStream.DefaultRetryDelayInMillis

  def connect(): Unit = {
    logger.debug("(re-)connecting stream")
    val listener = new SseListener(this)
    state = SseStreamState.Connecting(listener)

    lastEventId.foreach { lastEventId =>
      connectRequest.getHeaders.set(SseStream.LastEventIdHeaderName, lastEventId)
    }

    // [e]
    //
    // [e]
    httpEngine.executeRequest(
      connectRequest,
      originalSession.userId,
      shareConnections,
      originalSession.eventLoop,
      listener,
      userSslContexts
    )
  }

  def connected(): Unit =
    state match {
      case SseStreamState.Connecting(listener) =>
        logger.debug("Stream connected while in state Connecting. Processing.")
        state = SseStreamState.Connected(listener)
        fsm.onSseStreamConnected()

      case SseStreamState.Connected(listener) =>
        illegalState(listener, "Invalid state: stream was connected while state was Connected. Please report.")

      case SseStreamState.Closing(listener) =>
        logger.debug("Stream connected while state was Closing. Closing.")
        listener.closeChannel()
        fsm.onSseStreamClosed()
        state = SseStreamState.Closed

      case SseStreamState.Closed =>
        illegalState(null, "Invalid state: stream was connected while state was Closed. Please report.")
    }

  def endOfStream(): Unit =
    state match {
      case SseStreamState.Connecting(listener) =>
        illegalState(listener, "Invalid state: server ended the stream while state was Connecting. Please report.")

      case SseStreamState.Connected(_) =>
        logger.debug("End of stream reached while in state Connected.")
        state = SseStreamState.Closed
        fsm.onSseEndOfStream()

      case SseStreamState.Closing(_) =>
        state = SseStreamState.Closed // so everything gets garbage collected

      case SseStreamState.Closed => // already closed, do nothing
        logger.debug("End of stream reached while in state Closed.")
    }

  def closeFromClient(): Unit =
    state match {
      case SseStreamState.Connecting(listener) =>
        listener.closeChannel()
        state = SseStreamState.Closing(listener)
        fsm.onSseStreamClosed()

      case SseStreamState.Connected(listener) =>
        listener.closeChannel()
        state = SseStreamState.Closing(listener)
        fsm.onSseStreamClosed()

      case SseStreamState.Closing(listener) =>
        illegalState(listener, "Invalid state: client closed the stream while state was Closing. Please report.")

      case SseStreamState.Closed => // already closed, do nothing
    }

  def crash(throwable: Throwable): Unit = {
    if (logger.underlying.isDebugEnabled) {
      logger.debug("Sse stream crashed", throwable)
    } else {
      val errorMessage = throwable.rootMessage
      logger.error(s"Sse stream crashed: $errorMessage")
    }

    state match {
      case SseStreamState.Connecting(_) =>
        state = SseStreamState.Closed
        fsm.onSseStreamCrashed(throwable)

      case SseStreamState.Connected(_) =>
        if (throwable == PrematureCloseException.INSTANCE) {
          // reconnect
          originalSession.eventLoop.schedule(
            (() => connect()): Runnable,
            retryDelayInMillis,
            TimeUnit.MILLISECONDS
          )
        } else {
          state = SseStreamState.Closed
          fsm.onSseStreamCrashed(throwable)
        }

      case SseStreamState.Closing(_) =>
        state = SseStreamState.Closed

      case SseStreamState.Closed =>
      // weird but ignore
    }
  }

  def eventReceived(event: ServerSentEvent): Unit = {
    if (event.id.isDefined) {
      lastEventId = event.id
    }
    event.retry.foreach(retryDelayInMillis = _)

    state match {
      case SseStreamState.Connected(_) =>
        logger.debug(s"Received SSE event $event while in Open state. Propagating.")
        fsm.onSseReceived(event)
      case SseStreamState.Connecting(listener) =>
        illegalState(listener, s"Invalid state: received SSE $event while state was Connecting. Please report.")
      case SseStreamState.Closing(_) =>
        logger.debug(s"Received SSE event $event while in ProcessingClientCloseRequest state. Ignoring.")
      case _ =>
        illegalState(null, s"Invalid state: received SSE $event while state was Close. Please report.")
    }
  }

  private def illegalState(listener: SseListener, message: String): Unit = {
    fsm.onSseStreamCrashed(new IllegalStateException(message))
    if (listener != null) {
      listener.closeChannel()
    }
    state = SseStreamState.Closed
  }
}

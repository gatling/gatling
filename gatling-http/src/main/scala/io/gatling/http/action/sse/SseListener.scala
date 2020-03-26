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

package io.gatling.http.action.sse

import java.io.IOException

import io.gatling.commons.util.Clock
import io.gatling.commons.util.Throwables._
import io.gatling.core.stats.StatsEngine
import io.gatling.http.action.sse.fsm._
import io.gatling.http.client.HttpListener
import io.gatling.http.MissingNettyHttpHeaderValues

import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaders, HttpResponseStatus }

class SseInvalidStatusException(statusCode: Int) extends IOException(s"Server returned http response with code $statusCode") {
  override def fillInStackTrace(): Throwable = this
}

class SseInvalidContentTypeException(contentType: String) extends IOException(s"Server returned http response with content-type $contentType") {
  override def fillInStackTrace(): Throwable = this
}

class SseListener(fsm: SseFsm, statsEngine: StatsEngine, clock: Clock) extends HttpListener with SseStream with EventStreamDispatcher with StrictLogging {

  private var state: SseState = Connecting
  private val decoder = new SseStreamDecoder
  private var channel: Channel = _

  override def onWrite(channel: Channel): Unit =
    this.channel = channel

  override def onHttpResponse(status: HttpResponseStatus, headers: HttpHeaders): Unit = {

    val contentType = headers.get(HttpHeaderNames.CONTENT_TYPE)
    logger.debug(s"Status ${status.code} Content-Type $contentType received for SSE")

    if (status != HttpResponseStatus.OK) {
      val ex = new SseInvalidStatusException(status.code)
      onThrowable(ex)
      throw ex
    } else if (contentType != null && contentType.startsWith(MissingNettyHttpHeaderValues.TextEventStream.toString)) {
      state = Connected
      fsm.onSseStreamConnected(this, clock.nowMillis)
    } else {
      val ex = new SseInvalidContentTypeException(contentType)
      onThrowable(ex)
      throw ex
    }
  }

  override def onHttpResponseBodyChunk(chunk: ByteBuf, last: Boolean): Unit =
    if (state != Closed) {
      val events = decoder.decodeStream(chunk)
      events.foreach(dispatchEventStream)
      if (last) {
        close()
      }
    }

  override def onThrowable(throwable: Throwable): Unit =
    if (state != Closed) {
      close()
      sendOnThrowable(throwable)
    }

  def sendOnThrowable(throwable: Throwable): Unit = {
    val errorMessage = throwable.rootMessage

    if (logger.underlying.isDebugEnabled) {
      logger.debug("Request failed", throwable)
    } else {
      logger.info(s"Request failed: $errorMessage")
    }

    state match {
      case Connecting | Connected =>
        fsm.onSseStreamCrashed(throwable, clock.nowMillis)

      case Closed =>
        logger.error(s"unexpected state closed with error message: $errorMessage")
    }
  }

  override def close(): Unit =
    if (state != Closed) {
      state = Closed
      if (channel != null) {
        channel.close()
        channel = null
      }
      fsm.onSseStreamClosed(clock.nowMillis)
    }

  override def dispatchEventStream(sse: ServerSentEvent): Unit = fsm.onSseReceived(sse.asJsonString, clock.nowMillis)
}

private sealed trait SseState
private case object Connecting extends SseState
private case object Connected extends SseState
private case object Closed extends SseState

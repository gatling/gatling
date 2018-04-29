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

package io.gatling.http.action.sse

import java.io.IOException
import java.net.InetSocketAddress

import io.gatling.commons.util.Clock
import io.gatling.commons.util.Throwables._
import io.gatling.core.stats.StatsEngine
import io.gatling.http.action.sse.fsm._
import io.gatling.http.client.HttpListener

import akka.actor.ActorRef
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }

class SseException(statusCode: Int) extends IOException("Server returned http response with code " + statusCode) {
  override def fillInStackTrace(): Throwable = this
}

class SseListener(sseActor: ActorRef, statsEngine: StatsEngine, clock: Clock) extends HttpListener
  with SseStream
  with EventStreamDispatcher
  with StrictLogging {

  private var done = false
  private var state: SseState = Opening
  private val decoder = new SseStreamDecoder
  private var channel: Channel = _

  override def onTcpConnectSuccess(address: InetSocketAddress, channel: Channel): Unit = {
    state = Open
    this.channel = channel
  }

  override def onHttpResponse(status: HttpResponseStatus, headers: HttpHeaders): Unit = {

    logger.debug(s"Status ${status.code} received for SSE")

    if (status == HttpResponseStatus.OK) {
      sseActor ! SseStreamConnected(this, clock.nowMillis)

    } else {
      val ex = new SseException(status.code)
      onThrowable(ex)
      throw ex
    }
  }

  override def onHttpResponseBodyChunk(chunk: ByteBuf, last: Boolean): Unit =
    if (!done) {
      val events = decoder.decodeStream(chunk)
      events.foreach(dispatchEventStream)
      if (last) {
        close()
      }
    }

  override def onThrowable(throwable: Throwable): Unit =
    if (!done) {
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
      case Opening | Open =>
        sseActor ! SseStreamCrashed(throwable, clock.nowMillis)

      case Closed =>
        logger.error(s"unexpected state closed with error message: $errorMessage")
    }
  }

  override def close(): Unit = {
    done = true
    if (channel != null) {
      channel.close()
      channel = null
    }
  }

  override def dispatchEventStream(sse: ServerSentEvent): Unit = sseActor ! SseReceived(sse.asJsonString, clock.nowMillis)
}

private sealed trait SseState
private case object Opening extends SseState
private case object Open extends SseState
private case object Closed extends SseState

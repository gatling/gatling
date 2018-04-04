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

package io.gatling.http.action.async.sse

import java.io.IOException
import java.net.InetSocketAddress

import io.gatling.commons.util.Throwables._
import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.http.action.async.{ AsyncTx, OnFailedOpen }
import io.gatling.http.client.HttpListener

import akka.actor.ActorRef
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }

class SseException(statusCode: Int) extends IOException("Server returned http response with code " + statusCode) {
  override def fillInStackTrace(): Throwable = this
}

class SseListener(tx: AsyncTx, sseActor: ActorRef) extends HttpListener
  with SseStream
  with EventStreamDispatcher
  with StrictLogging {

  private var done = false
  private var state: SseState = Opening
  private val decoder = new SseStreamDecoder

  override def onTcpConnectSuccess(address: InetSocketAddress, connection: Channel): Unit =
    state = Open

  override def onHttpResponse(status: HttpResponseStatus, headers: HttpHeaders): Unit = {

    logger.debug(s"Status ${status.code} received for SSE '${tx.requestName}")

    if (status == HttpResponseStatus.OK) {
      sseActor ! OnOpen(tx, this, nowMillis)

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
      done = true
      sendOnThrowable(throwable)
    }

  def sendOnThrowable(throwable: Throwable): Unit = {
    val errorMessage = throwable.rootMessage

    if (logger.underlying.isDebugEnabled) {
      logger.debug(s"Request '${tx.requestName}' failed for user ${tx.session.userId}", throwable)
    } else {
      logger.info(s"Request '${tx.requestName}' failed for user ${tx.session.userId}: $errorMessage")
    }

    state match {
      case Opening =>
        sseActor ! OnFailedOpen(tx, errorMessage, nowMillis)

      case Open =>
        sseActor ! OnThrowable(tx, errorMessage, nowMillis)

      case Closed =>
        logger.error(s"unexpected state closed with error message: $errorMessage")
    }
  }

  override def close(): Unit = {
    done = true
    sseActor ! OnClose
  }

  override def dispatchEventStream(sse: ServerSentEvent): Unit = sseActor ! OnMessage(sse.asJsonString, nowMillis)
}

private sealed trait SseState
private case object Opening extends SseState
private case object Open extends SseState
private case object Closed extends SseState

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

package io.gatling.http.action.sse

import java.io.IOException

import io.gatling.http.MissingNettyHttpHeaderValues
import io.gatling.http.action.sse.fsm._
import io.gatling.http.client.HttpListener

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

class SseListener(stream: SseStream) extends HttpListener with StrictLogging {

  private val decoder = new SseStreamDecoder
  private var channel: Channel = _
  private var closed = false

  override def onWrite(channel: Channel): Unit =
    this.channel = channel

  def closeChannel(): Unit =
    if (channel != null) {
      closed = true
      channel.close()
      channel == null
    }

  override def onHttpResponse(status: HttpResponseStatus, headers: HttpHeaders): Unit =
    if (!closed) {
      val contentType = headers.get(HttpHeaderNames.CONTENT_TYPE)
      logger.debug(s"Status ${status.code} Content-Type $contentType received for SSE")

      status match {
        case HttpResponseStatus.OK =>
          if (contentType != null && contentType.startsWith(MissingNettyHttpHeaderValues.TextEventStream.toString)) {
            stream.connected()

          } else {
            onThrowable(new SseInvalidContentTypeException(contentType))
          }

        case HttpResponseStatus.NO_CONTENT =>
          stream.endOfStream()
          closeChannel()

        case _ => onThrowable(new SseInvalidStatusException(status.code))
      }
    }

  override def onHttpResponseBodyChunk(chunk: ByteBuf, last: Boolean): Unit =
    if (!closed) {
      val events = decoder.decodeStream(chunk)
      events.foreach(stream.eventReceived)
      if (last) {
        stream.closedByServer()
      }
    }

  override def onThrowable(throwable: Throwable): Unit =
    if (!closed) {
      closeChannel()
      stream.crash(throwable)
    }
}

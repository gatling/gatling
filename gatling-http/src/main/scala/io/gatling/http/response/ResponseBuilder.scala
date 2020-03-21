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

package io.gatling.http.response

import java.nio.charset.Charset

import scala.math.max
import scala.util.control.NonFatal

import io.gatling.commons.util.Maps._
import io.gatling.commons.util.StringHelper.bytes2Hex
import io.gatling.commons.util.Throwables._
import io.gatling.http.HeaderNames
import io.gatling.http.util.HttpHelper.{ extractCharsetFromContentType, isCss, isHtml }
import io.gatling.http.request.HttpRequest

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.{ EmptyHttpHeaders, HttpHeaders, HttpResponseStatus }

class ResponseBuilder(request: HttpRequest) {

  import request.requestConfig._

  private var storeHtmlOrCss: Boolean = _
  private var startTimestamp: Long = _
  private var endTimestamp: Long = _
  private var isHttp2: Boolean = _
  private var status: HttpResponseStatus = _
  private var wireRequestHeaders: HttpHeaders = EmptyHttpHeaders.INSTANCE

  private var headers: HttpHeaders = EmptyHttpHeaders.INSTANCE
  private var chunks: List[ByteBuf] = Nil

  def updateStart(startTimestamp: Long, wireRequestHeaders: HttpHeaders): Unit = {
    this.startTimestamp = startTimestamp
    this.wireRequestHeaders = wireRequestHeaders
  }

  def updateEndTimestamp(endTimestamp: Long): Unit =
    this.endTimestamp = endTimestamp

  def recordResponse(status: HttpResponseStatus, headers: HttpHeaders, timestamp: Long): Unit = {
    updateEndTimestamp(timestamp)

    this.status = status
    this.headers = headers
    storeHtmlOrCss = httpProtocol.responsePart.inferHtmlResources && (isHtml(headers) || isCss(headers))
  }

  def recordBodyChunk(byteBuf: ByteBuf, timestamp: Long): Unit = {
    updateEndTimestamp(timestamp)

    if (byteBuf.isReadable) {
      if (storeBodyParts || storeHtmlOrCss) {
        chunks = byteBuf.retain() :: chunks // beware, we have to retain!
      }

      if (digests.nonEmpty)
        for {
          nioBuffer <- byteBuf.nioBuffers
          digest <- digests.values
        } digest.update(nioBuffer.duplicate)
    }
  }

  def setHttp2(isHttp2: Boolean): Unit = this.isHttp2 = isHttp2

  private def resolveCharset: Charset = {
    val contentTypeHeader = headers.get(HeaderNames.ContentType)
    if (contentTypeHeader == null) {
      defaultCharset
    } else {
      extractCharsetFromContentType(contentTypeHeader).getOrElse(defaultCharset)
    }
  }

  def buildResponse: HttpResult =
    if (status == null) {
      buildFailure("How come we're trying to build a response with no status?!")
    } else {
      try {
        // Clock source might not be monotonic.
        // Moreover, ProgressListener might be called AFTER ChannelHandler methods
        // ensure response doesn't end before starting
        endTimestamp = max(endTimestamp, startTimestamp)

        val checksums = digests.forceMapValues(md => bytes2Hex(md.digest))

        val chunksOrderedByArrival = chunks.reverse
        val body = ResponseBody(chunksOrderedByArrival, resolveCharset)

        Response(
          request.clientRequest,
          wireRequestHeaders,
          startTimestamp,
          endTimestamp,
          status,
          headers,
          body,
          checksums,
          isHttp2
        )
      } catch {
        case NonFatal(t) => buildFailure(t)
      }
    }

  def buildFailure(t: Throwable): HttpFailure = buildFailure(t.detailedMessage)

  private def buildFailure(errorMessage: String): HttpFailure =
    HttpFailure(request.clientRequest, wireRequestHeaders, startTimestamp, endTimestamp, errorMessage)

  def releaseChunks(): Unit = {
    chunks.foreach(_.release())
    chunks = Nil
  }
}

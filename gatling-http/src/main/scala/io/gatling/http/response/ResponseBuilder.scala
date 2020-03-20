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

  var storeHtmlOrCss: Boolean = _
  var startTimestamp: Long = _
  var endTimestamp: Long = _
  private var isHttp2: Boolean = _
  private var status: Option[HttpResponseStatus] = None
  private var wireRequestHeaders: HttpHeaders = EmptyHttpHeaders.INSTANCE

  private var headers: HttpHeaders = EmptyHttpHeaders.INSTANCE
  private var chunks: List[ByteBuf] = Nil

  def updateStart(startTimestamp: Long, wireRequestHeaders: HttpHeaders): Unit = {
    this.startTimestamp = startTimestamp
    this.wireRequestHeaders = wireRequestHeaders
  }

  def updateEndTimestamp(endTimestamp: Long): Unit =
    this.endTimestamp = endTimestamp

  def accumulate(status: HttpResponseStatus, headers: HttpHeaders, timestamp: Long): Unit = {
    updateEndTimestamp(timestamp)

    this.status = Some(status)
    if (this.headers eq EmptyHttpHeaders.INSTANCE) {
      this.headers = headers
      storeHtmlOrCss = httpProtocol.responsePart.inferHtmlResources && (isHtml(headers) || isCss(headers))
    } else {
      // trailing headers, wouldn't contain ContentType
      this.headers.add(headers)
    }
  }

  def accumulate(byteBuf: ByteBuf, timestamp: Long): Unit = {
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

  private def computeContentLength: Int = {
    var l = 0
    chunks.foreach(l += _.readableBytes)
    l
  }

  def buildResponse: HttpResult =
    status match {
      case Some(s) =>
        try {
          // Clock source might not be monotonic.
          // Moreover, ProgressListener might be called AFTER ChannelHandler methods
          // ensure response doesn't end before starting
          endTimestamp = max(endTimestamp, startTimestamp)

          val checksums = digests.forceMapValues(md => bytes2Hex(md.digest))

          val resolvedCharset = resolveCharset

          val chunksOrderedByArrival = chunks.reverse
          val body: ResponseBody = ResponseBody(chunksOrderedByArrival, resolvedCharset)

          Response(
            request.clientRequest,
            wireRequestHeaders,
            startTimestamp,
            endTimestamp,
            s,
            headers,
            body,
            checksums,
            computeContentLength,
            resolvedCharset,
            isHttp2
          )
        } catch {
          case NonFatal(t) => buildFailure(t)
        }
      case _ => buildFailure("How come we're trying to build a response with no status?!")
    }

  def buildFailure(t: Throwable): HttpFailure = buildFailure(t.detailedMessage)

  private def buildFailure(errorMessage: String): HttpFailure =
    HttpFailure(request.clientRequest, wireRequestHeaders, startTimestamp, endTimestamp, errorMessage)

  def releaseChunks(): Unit = {
    chunks.foreach(_.release())
    chunks = Nil
  }
}

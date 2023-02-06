/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.http.engine

import java.nio.charset.Charset

import scala.math.max
import scala.util.control.NonFatal

import io.gatling.commons.util.Clock
import io.gatling.commons.util.Hex.toHexString
import io.gatling.commons.util.Throwables._
import io.gatling.http.client.HttpListener
import io.gatling.http.engine.response.ResponseProcessor
import io.gatling.http.engine.tx.HttpTx
import io.gatling.http.response.{ HttpFailure, HttpResult, Response, ResponseBody }
import io.gatling.http.util.HttpHelper.{ extractCharsetFromContentType, isCss, isHtml }

import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.{ EmptyHttpHeaders, HttpHeaderNames, HttpHeaders, HttpResponseStatus }

object GatlingHttpListener extends StrictLogging {
  private def resolveCharset(headers: HttpHeaders, defaultCharset: Charset): Charset = {
    val contentTypeHeader = headers.get(HttpHeaderNames.CONTENT_TYPE)
    if (contentTypeHeader == null) {
      defaultCharset
    } else {
      extractCharsetFromContentType(contentTypeHeader).getOrElse(defaultCharset)
    }
  }

  private def logRequestCrash(tx: HttpTx, throwable: Throwable): Unit =
    logger.debug(s"Request '${tx.request.requestName}' failed for user ${tx.session.userId}", throwable)

  // [e]
  //
  // [e]
}

class GatlingHttpListener(tx: HttpTx, clock: Clock, responseProcessor: ResponseProcessor) extends HttpListener {
  import GatlingHttpListener._
  import tx.request.requestConfig._

  private var init = false
  private var done = false
  private var storeHtmlOrCss: Boolean = _
  private var requestStartTimestamp: Long = _
  private var requestEndTimestamp: Long = _
  private var isHttp2: Boolean = _
  private var status: HttpResponseStatus = _
  private var headers: HttpHeaders = EmptyHttpHeaders.INSTANCE
  private var bodyLength = 0
  private var chunks: List[ByteBuf] = Nil
  private val digests = checksumAlgorithms.map(algorithm => algorithm -> algorithm.digest).toMap

  override def onSend(): Unit =
    if (!init) {
      init = true
      requestStartTimestamp = clock.nowMillis
      // [e]
      //
      //
      //
      // [e]
    }

  // [e]
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  // [e]

  override def onProtocolAwareness(isHttp2: Boolean): Unit =
    this.isHttp2 = isHttp2

  override def onHttpResponse(status: HttpResponseStatus, headers: HttpHeaders): Unit =
    if (!done) {
      requestEndTimestamp = clock.nowMillis
      this.status = status
      this.headers = headers
      storeHtmlOrCss = httpProtocol.responsePart.inferHtmlResources && (isHtml(headers) || isCss(headers))
    }

  override def onHttpResponseBodyChunk(chunk: ByteBuf, last: Boolean): Unit =
    if (!done) {
      requestEndTimestamp = clock.nowMillis

      val chunkLength = chunk.readableBytes
      if (chunkLength > 0) {
        bodyLength += chunkLength
        if (storeBodyParts || storeHtmlOrCss) {
          // beware, we have to retain!
          chunks = chunk.retain() :: chunks
        }

        if (digests.nonEmpty)
          for {
            nioBuffer <- chunk.nioBuffers
            digest <- digests.values
          } digest.update(nioBuffer.duplicate)
      }

      if (last) {
        done = true
        try {
          responseProcessor.onComplete(buildResponse)
        } finally {
          releaseChunks()
        }
      }
    }

  private def buildResponse: HttpResult =
    if (status == null) {
      buildFailure("How come we're trying to build a response with no status?!")
    } else {
      try {
        // Clock source might not be monotonic.
        // ensure response doesn't end before starting
        requestEndTimestamp = max(requestEndTimestamp, requestStartTimestamp)

        val checksums = digests.view.mapValues(md => toHexString(md.digest)).to(Map)

        val chunksOrderedByArrival = chunks.reverse
        val body = ResponseBody(bodyLength, chunksOrderedByArrival, resolveCharset(headers, defaultCharset))

        Response(
          tx.request.clientRequest,
          requestStartTimestamp,
          requestEndTimestamp,
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

  private def buildFailure(t: Throwable): HttpFailure = {
    val rootCause = t.rootCause
    val rootCauseClass = rootCause.getClass
    val message =
      if (rootCauseClass eq classOf[io.netty.handler.ssl.NotSslRecordException]) {
        "i.n.h.s.NotSslRecordException"
      } else if (rootCauseClass eq classOf[io.gatling.http.client.impl.RequestTimeoutException]) {
        rootCause.getMessage
      } else {
        rootCause.detailedMessage
      }

    buildFailure(message)
  }

  private def buildFailure(errorMessage: String): HttpFailure =
    HttpFailure(
      tx.request.clientRequest,
      requestStartTimestamp,
      requestEndTimestamp,
      errorMessage
    )

  private def releaseChunks(): Unit = {
    chunks.foreach(_.release())
    chunks = Nil
  }

  override def onThrowable(throwable: Throwable): Unit =
    if (!done) {
      done = true
      requestEndTimestamp = clock.nowMillis
      logRequestCrash(tx, throwable)
      try {
        responseProcessor.onComplete(buildFailure(throwable))
      } finally {
        releaseChunks()
      }
    }
}

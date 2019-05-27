/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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
import java.security.MessageDigest

import scala.collection.breakOut
import scala.math.max
import scala.util.control.NonFatal

import io.gatling.commons.util.Clock
import io.gatling.commons.util.Collections._
import io.gatling.commons.util.Maps._
import io.gatling.commons.util.StringHelper.bytes2Hex
import io.gatling.commons.util.Throwables._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HeaderNames
import io.gatling.http.check.checksum.ChecksumCheck
import io.gatling.http.client.Request
import io.gatling.http.engine.response._
import io.gatling.http.util.HttpHelper.{ extractCharsetFromContentType, isCss, isHtml, isTxt }

import com.typesafe.scalalogging.StrictLogging
import io.gatling.http.request.HttpRequestConfig
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.{ EmptyHttpHeaders, HttpHeaders, HttpResponseStatus }

object ResponseBuilder extends StrictLogging {

  def newResponseBuilderFactory(
    requestConfig: HttpRequestConfig,
    clock:         Clock,
    configuration: GatlingConfiguration
  ): ResponseBuilderFactory = {

    val checksumChecks = requestConfig.checks.collect {
      case checksumCheck: ChecksumCheck => checksumCheck
    }

    val bodyUsageStrategies =
      if (requestConfig.responseTransformer.isDefined) {
        // we can't assume anything about if and how the response body will be used,
        // let's force bytes so we don't risk decoding binary content
        List(ByteArrayResponseBodyUsage)
      } else {
        requestConfig.checks.flatMap(_.responseBodyUsage)
      }

    val storeBodyParts = IsHttpDebugEnabled || bodyUsageStrategies.nonEmpty

    request => new ResponseBuilder(
      request,
      checksumChecks,
      bodyUsageStrategies,
      storeBodyParts,
      requestConfig.httpProtocol.responsePart.inferHtmlResources,
      configuration.core.charset,
      clock
    )
  }
}

class ResponseBuilder(
    request:            Request,
    checksumChecks:     List[ChecksumCheck],
    bodyUsages:         Seq[ResponseBodyUsage],
    storeBodyParts:     Boolean,
    inferHtmlResources: Boolean,
    defaultCharset:     Charset,
    clock:              Clock
) {

  private val computeChecksums = checksumChecks.nonEmpty
  var storeHtmlOrCss: Boolean = _
  var startTimestamp: Long = _
  var endTimestamp: Long = _
  private var isHttp2: Boolean = _
  private var status: Option[HttpResponseStatus] = None
  private var wireRequestHeaders: HttpHeaders = EmptyHttpHeaders.INSTANCE

  private var headers: HttpHeaders = EmptyHttpHeaders.INSTANCE
  private var chunks: List[ByteBuf] = Nil
  private val digests: Map[String, MessageDigest] =
    if (computeChecksums)
      checksumChecks.map(check => check.algorithm -> MessageDigest.getInstance(check.algorithm))(breakOut)
    else
      Map.empty

  def updateStartTimestamp(): Unit =
    startTimestamp = clock.nowMillis

  def updateEndTimestamp(): Unit =
    endTimestamp = clock.nowMillis

  def accumulate(wireRequestHeaders: HttpHeaders): Unit =
    this.wireRequestHeaders = wireRequestHeaders

  def accumulate(status: HttpResponseStatus, headers: HttpHeaders): Unit = {
    updateEndTimestamp()

    this.status = Some(status)
    if (this.headers eq EmptyHttpHeaders.INSTANCE) {
      this.headers = headers
      storeHtmlOrCss = inferHtmlResources && (isHtml(headers) || isCss(headers))
    } else {
      // trailing headers, wouldn't contain ContentType
      this.headers.add(headers)
    }
  }

  def setHttp2(isHttp2: Boolean): Unit = this.isHttp2 = isHttp2

  def accumulate(byteBuf: ByteBuf): Unit = {
    updateEndTimestamp()

    if (byteBuf.isReadable) {
      if (storeBodyParts || storeHtmlOrCss) {
        chunks = byteBuf.retain() :: chunks // beware, we have to retain!
      }

      if (computeChecksums)
        for {
          nioBuffer <- byteBuf.nioBuffers
          digest <- digests.values
        } digest.update(nioBuffer.duplicate)
    }
  }

  private def resolveCharset: Charset = Option(headers.get(HeaderNames.ContentType))
    .flatMap(extractCharsetFromContentType)
    .getOrElse(defaultCharset)

  def buildResponse: HttpResult =
    try {
      status match {
        case Some(s) =>
          try {
            // Clock source might not be monotonic.
            // Moreover, ProgressListener might be called AFTER ChannelHandler methods
            // ensure response doesn't end before starting
            endTimestamp = max(endTimestamp, startTimestamp)

            val checksums = digests.forceMapValues(md => bytes2Hex(md.digest))

            val contentLength = chunks.sumBy(_.readableBytes)

            val resolvedCharset = resolveCharset

            val chunksOrderedByArrival = chunks.reverse
            val body: ResponseBody =
              if (chunksOrderedByArrival.isEmpty) {
                NoResponseBody

              } else if (bodyUsages.contains(ByteArrayResponseBodyUsage)) {
                // bodyBytes
                ByteArrayResponseBody(chunksOrderedByArrival, resolvedCharset)

              } else if (bodyUsages.contains(InputStreamResponseBodyUsage)) {
                // jsonPath
                // xpath
                InputStreamResponseBody(chunksOrderedByArrival, resolvedCharset)

              } else if (bodyUsages.contains(StringResponseBodyUsage)) {
                // jsonpJsonPath
                // regex
                // bodyString
                // substring
                StringResponseBody(chunksOrderedByArrival, resolvedCharset)

              } else if (bodyUsages.contains(CharArrayResponseBodyUsage)) {
                // css
                CharArrayResponseBody(chunksOrderedByArrival, resolvedCharset)

              } else if (isTxt(headers)) {
                StringResponseBody(chunksOrderedByArrival, resolvedCharset)

              } else {
                ByteArrayResponseBody(chunksOrderedByArrival, resolvedCharset)
              }

            Response(request, wireRequestHeaders, s, headers, body, checksums, contentLength, resolvedCharset, startTimestamp, endTimestamp, isHttp2)
          } catch {
            case NonFatal(t) => buildFailure(t)
          }
        case _ => buildFailure("How come we're trying to build a response with no status?!")
      }
    } finally {
      releaseChunks()
    }

  def buildFailure(t: Throwable): HttpFailure =
    try {
      buildFailure(t.detailedMessage)
    } finally {
      releaseChunks()
    }

  private def buildFailure(errorMessage: String): HttpFailure =
    HttpFailure(request, wireRequestHeaders, startTimestamp, endTimestamp, errorMessage)

  private def releaseChunks(): Unit = {
    chunks.foreach(_.release())
    chunks = Nil
  }
}

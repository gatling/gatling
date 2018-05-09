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

package io.gatling.http.response

import java.nio.charset.Charset
import java.security.MessageDigest

import scala.collection.breakOut
import scala.math.max

import io.gatling.commons.util.Clock
import io.gatling.commons.util.Collections._
import io.gatling.commons.util.Maps._
import io.gatling.commons.util.StringHelper.bytes2Hex
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HeaderNames
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.checksum.ChecksumCheck
import io.gatling.http.client.Request
import io.gatling.http.util.HttpHelper.{ extractCharsetFromContentType, isCss, isHtml, isTxt }

import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpHeaders, HttpResponseStatus }

object ResponseBuilder extends StrictLogging {

  val EmptyHeaders = new DefaultHttpHeaders

  val Identity: Response => Response = identity[Response]

  private val IsDebugEnabled = logger.underlying.isDebugEnabled

  def newResponseBuilderFactory(
    checks:                List[HttpCheck],
    responseTransformer:   Option[PartialFunction[Response, Response]],
    discardResponseChunks: Boolean,
    inferHtmlResources:    Boolean,
    clock:                 Clock,
    configuration:         GatlingConfiguration
  ): ResponseBuilderFactory = {

    val checksumChecks = checks.collect {
      case checksumCheck: ChecksumCheck => checksumCheck
    }

    val responseBodyUsageStrategies = checks.flatMap(_.responseBodyUsageStrategy).toSet

    val storeBodyParts = IsDebugEnabled || !discardResponseChunks || responseBodyUsageStrategies.nonEmpty || responseTransformer.isDefined

    request => new ResponseBuilder(
      request,
      checksumChecks,
      responseBodyUsageStrategies,
      responseTransformer,
      storeBodyParts,
      inferHtmlResources,
      configuration.core.charset,
      clock
    )
  }
}

class ResponseBuilder(
    request:             Request,
    checksumChecks:      List[ChecksumCheck],
    bodyUsageStrategies: Set[ResponseBodyUsageStrategy],
    responseTransformer: Option[PartialFunction[Response, Response]],
    storeBodyParts:      Boolean,
    inferHtmlResources:  Boolean,
    defaultCharset:      Charset,
    clock:               Clock
) {

  private val computeChecksums = checksumChecks.nonEmpty
  var storeHtmlOrCss: Boolean = _
  var startTimestamp: Long = _
  var endTimestamp: Long = _
  private var status: Option[HttpResponseStatus] = None
  private var wireRequestHeaders: Option[HttpHeaders] = None

  private var headers: HttpHeaders = ResponseBuilder.EmptyHeaders
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

  def accumulate(wireRequestHeaders: HttpHeaders): Unit = {
    this.wireRequestHeaders = Some(wireRequestHeaders)
  }

  def accumulate(status: HttpResponseStatus, headers: HttpHeaders): Unit = {
    updateEndTimestamp()

    this.status = Some(status)
    if (this.headers eq ResponseBuilder.EmptyHeaders) {
      this.headers = headers
      storeHtmlOrCss = inferHtmlResources && (isHtml(headers) || isCss(headers))
    } else {
      // trailing headers, wouldn't contain ContentType
      this.headers.add(headers)
    }
  }

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

  private def resolvedCharset: Charset = Option(headers.get(HeaderNames.ContentType))
    .flatMap(extractCharsetFromContentType)
    .getOrElse(defaultCharset)

  def build: Response = {

    // time measurement is imprecise due to multi-core nature
    // moreover, ProgressListener might be called AFTER ChannelHandler methods
    // ensure response doesn't end before starting
    endTimestamp = max(endTimestamp, startTimestamp)

    val checksums = digests.forceMapValues(md => bytes2Hex(md.digest))

    val contentLength = chunks.sumBy(_.readableBytes)

    val bodyUsages = bodyUsageStrategies.map(_.bodyUsage(contentLength))

    val resolvedCharset = Option(headers.get(HeaderNames.ContentType))
      .flatMap(extractCharsetFromContentType)
      .getOrElse(defaultCharset)

    val properlyOrderedChunks = chunks.reverse
    val body: ResponseBody =
      if (properlyOrderedChunks.isEmpty)
        NoResponseBody

      else if (bodyUsages.contains(ByteArrayResponseBodyUsage))
        ByteArrayResponseBody(properlyOrderedChunks, resolvedCharset)

      else if (bodyUsages.contains(InputStreamResponseBodyUsage))
        InputStreamResponseBody(properlyOrderedChunks, resolvedCharset)

      else if (bodyUsages.contains(StringResponseBodyUsage))
        StringResponseBody(properlyOrderedChunks, resolvedCharset)

      else if (bodyUsages.contains(CharArrayResponseBodyUsage))
        CharArrayResponseBody(properlyOrderedChunks, resolvedCharset)

      else if (isTxt(headers))
        StringResponseBody(properlyOrderedChunks, resolvedCharset)

      else
        ByteArrayResponseBody(properlyOrderedChunks, resolvedCharset)

    chunks.foreach(_.release())
    chunks = Nil
    val rawResponse = HttpResponse(request, wireRequestHeaders, status, headers, body, checksums, contentLength, resolvedCharset, startTimestamp, endTimestamp)

    responseTransformer match {
      case Some(transformer) => transformer.applyOrElse(rawResponse, ResponseBuilder.Identity)
      case _                 => rawResponse
    }
  }

  def buildSafeResponse: Response =
    HttpResponse(request, wireRequestHeaders, status, headers, NoResponseBody, Map.empty, 0, resolvedCharset, startTimestamp, endTimestamp)
}

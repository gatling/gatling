/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConversions._
import scala.math.max

import io.gatling.commons.util.StringHelper.bytes2Hex
import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.http.HeaderNames
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.checksum.ChecksumCheck
import io.gatling.http.util.HttpHelper.{ extractCharsetFromContentType, isCss, isHtml, isTxt }

import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.{ HttpHeaders, DefaultHttpHeaders }
import org.asynchttpclient._
import org.asynchttpclient.netty.request.NettyRequest
import org.asynchttpclient.netty.LazyNettyResponseBodyPart

object ResponseBuilder extends StrictLogging {

  val EmptyHeaders = new DefaultHttpHeaders

  val Identity: Response => Response = identity[Response]

  private val IsDebugEnabled = logger.underlying.isDebugEnabled

  def newResponseBuilderFactory(
    checks:                List[HttpCheck],
    responseTransformer:   Option[PartialFunction[Response, Response]],
    discardResponseChunks: Boolean,
    inferHtmlResources:    Boolean
  )(implicit configuration: GatlingConfiguration): ResponseBuilderFactory = {

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
      configuration.core.charset
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
    charset:             Charset
) {

  val computeChecksums = checksumChecks.nonEmpty
  @volatile var storeHtmlOrCss: Boolean = _
  @volatile var startTimestamp: Long = _
  @volatile var endTimestamp: Long = _
  @volatile private var status: Option[HttpResponseStatus] = None
  @volatile private var headers: HttpHeaders = ResponseBuilder.EmptyHeaders
  private val chunks = new ConcurrentLinkedQueue[ByteBuf]
  @volatile private var digests: Map[String, MessageDigest] = initDigests()
  @volatile private var nettyRequest: Option[NettyRequest] = None

  def initDigests(): Map[String, MessageDigest] =
    if (computeChecksums)
      checksumChecks.foldLeft(Map.empty[String, MessageDigest]) { (map, check) =>
        map + (check.algorithm -> MessageDigest.getInstance(check.algorithm))
      }
    else
      Map.empty[String, MessageDigest]

  def updateStartTimestamp(): Long = {
    startTimestamp = nowMillis
    startTimestamp
  }

  def setNettyRequest(nettyRequest: NettyRequest) =
    this.nettyRequest = Some(nettyRequest)

  def reset(): Unit = {
    endTimestamp = 0L
    status = None
    headers = ResponseBuilder.EmptyHeaders
    chunks.foreach(_.release())
    chunks.clear()
    digests = initDigests()
  }

  def updateEndTimestamp(): Unit = endTimestamp = nowMillis

  def accumulate(status: HttpResponseStatus): Unit = {
    this.status = Some(status)
    endTimestamp = nowMillis
  }

  def accumulate(headers: HttpResponseHeaders): Unit = {
    this.headers = headers.getHeaders
    storeHtmlOrCss = inferHtmlResources && (isHtml(headers.getHeaders) || isCss(headers.getHeaders))
    updateEndTimestamp()
  }

  def accumulate(bodyPart: HttpResponseBodyPart): Unit = {

    updateEndTimestamp()

    val byteBuf = bodyPart.asInstanceOf[LazyNettyResponseBodyPart].getBuf

    if (byteBuf.readableBytes > 0) {
      if (storeBodyParts || storeHtmlOrCss) {
        chunks += byteBuf.retain() // beware, we have to retain!
      }

      if (computeChecksums)
        for {
          nioBuffer <- byteBuf.nioBuffers
          digest <- digests.values
        } digest.update(nioBuffer.duplicate)
    }
  }

  def build: Response = {

    // time measurement is imprecise due to multi-core nature
    // moreover, ProgressListener might be called AFTER ChannelHandler methods 
    // ensure response doesn't end before starting
    endTimestamp = max(endTimestamp, startTimestamp)

    val checksums = digests.foldLeft(Map.empty[String, String]) { (map, entry) =>
      val (algo, md) = entry
      map + (algo -> bytes2Hex(md.digest))
    }

    val bodyLength = chunks.foldLeft(0) { (sum, chunk) =>
      sum + chunk.readableBytes
    }

    val bodyUsages = bodyUsageStrategies.map(_.bodyUsage(bodyLength))

    val resolvedCharset = Option(headers.get(HeaderNames.ContentType))
      .flatMap(extractCharsetFromContentType)
      .getOrElse(charset)

    val body: ResponseBody =
      if (chunks.isEmpty)
        NoResponseBody

      else if (bodyUsages.contains(ByteArrayResponseBodyUsage))
        ByteArrayResponseBody(chunks, resolvedCharset)

      else if (bodyUsages.contains(InputStreamResponseBodyUsage) || bodyUsages.isEmpty)
        InputStreamResponseBody(chunks, resolvedCharset)

      else if (isTxt(headers))
        StringResponseBody(chunks, resolvedCharset)

      else
        ByteArrayResponseBody(chunks, resolvedCharset)

    chunks.foreach(_.release())
    val timings = ResponseTimings(startTimestamp, endTimestamp)
    val rawResponse = HttpResponse(request, nettyRequest, status, headers, body, checksums, bodyLength, resolvedCharset, timings)

    responseTransformer match {
      case None              => rawResponse
      case Some(transformer) => transformer.applyOrElse(rawResponse, ResponseBuilder.Identity)
    }
  }
}

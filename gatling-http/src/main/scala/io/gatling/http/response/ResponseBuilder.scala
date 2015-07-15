/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.net.InetAddress
import java.nio.charset.Charset
import java.security.MessageDigest

import scala.collection.mutable.ArrayBuffer
import scala.math.max

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.util.StringHelper.bytes2Hex
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.http.HeaderNames
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.checksum.ChecksumCheck
import io.gatling.http.util.HttpHelper.{ extractCharsetFromContentType, isCss, isHtml, isTxt }

import com.typesafe.scalalogging.StrictLogging
import org.asynchttpclient._
import org.asynchttpclient.netty.request.NettyRequest
import org.asynchttpclient.netty.NettyResponseBodyPart
import org.jboss.netty.buffer.ChannelBuffer

object ResponseBuilder extends StrictLogging {

  val EmptyHeaders = new FluentCaseInsensitiveStringsMap

  val Identity: Response => Response = identity[Response]

  private val IsDebugEnabled = logger.underlying.isDebugEnabled

  def newResponseBuilderFactory(checks: List[HttpCheck],
                                responseTransformer: Option[PartialFunction[Response, Response]],
                                discardResponseChunks: Boolean,
                                inferHtmlResources: Boolean)(implicit configuration: GatlingConfiguration): ResponseBuilderFactory = {

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
      configuration.core.charset)
  }
}

class ResponseBuilder(request: Request,
                      checksumChecks: List[ChecksumCheck],
                      bodyUsageStrategies: Set[ResponseBodyUsageStrategy],
                      responseTransformer: Option[PartialFunction[Response, Response]],
                      storeBodyParts: Boolean,
                      inferHtmlResources: Boolean,
                      charset: Charset) {

  val computeChecksums = checksumChecks.nonEmpty
  var storeHtmlOrCss = false
  var firstByteSent = nowMillis
  var lastByteReceived = 0L
  private var status: Option[HttpResponseStatus] = None
  private var headers: FluentCaseInsensitiveStringsMap = ResponseBuilder.EmptyHeaders
  private val chunks = new ArrayBuffer[ChannelBuffer]
  private var digests: Map[String, MessageDigest] = initDigests()
  private var nettyRequest: Option[NettyRequest] = None
  private var remoteAddress: Option[InetAddress] = None

  def initDigests(): Map[String, MessageDigest] =
    if (computeChecksums)
      checksumChecks.foldLeft(Map.empty[String, MessageDigest]) { (map, check) =>
        map + (check.algorithm -> MessageDigest.getInstance(check.algorithm))
      }
    else
      Map.empty[String, MessageDigest]

  def updateFirstByteSent(): Unit = firstByteSent = nowMillis

  def setNettyRequest(nettyRequest: NettyRequest) =
    this.nettyRequest = Some(nettyRequest)

  def setRemoteAddress(remoteAddress: InetAddress) =
    this.remoteAddress = Some(remoteAddress)

  def reset(): Unit = {
    firstByteSent = nowMillis
    lastByteReceived = 0L
    status = None
    headers = ResponseBuilder.EmptyHeaders
    chunks.clear()
    digests = initDigests()
  }

  def updateLastByteSent(): Unit = {}

  def updateLastByteReceived(): Unit = lastByteReceived = nowMillis

  def accumulate(status: HttpResponseStatus): Unit = {
    this.status = Some(status)
    lastByteReceived = nowMillis
  }

  def accumulate(headers: HttpResponseHeaders): Unit = {
    this.headers = headers.getHeaders
    storeHtmlOrCss = inferHtmlResources && (isHtml(headers.getHeaders) || isCss(headers.getHeaders))
    updateLastByteReceived()
  }

  def accumulate(bodyPart: HttpResponseBodyPart): Unit = {

    updateLastByteReceived()

    val channelBuffer = bodyPart.asInstanceOf[NettyResponseBodyPart].getChannelBuffer

    if (storeBodyParts || storeHtmlOrCss)
      chunks += channelBuffer

    if (computeChecksums)
      digests.values.foreach(_.update(bodyPart.getBodyByteBuffer))
  }

  def build: Response = {

    // time measurement is imprecise due to multi-core nature
    // moreover, ProgressListener might be called AFTER ChannelHandler methods 
    // ensure response doesn't end before starting
    lastByteReceived = max(lastByteReceived, firstByteSent)

    val checksums = digests.foldLeft(Map.empty[String, String]) { (map, entry) =>
      val (algo, md) = entry
      map + (algo -> bytes2Hex(md.digest))
    }

    val bodyLength = chunks.foldLeft(0) { (sum, chunk) =>
      sum + chunk.readableBytes
    }

    val bodyUsages = bodyUsageStrategies.map(_.bodyUsage(bodyLength))

    val resolvedCharset = Option(headers.getFirstValue(HeaderNames.ContentType))
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

    val timings = ResponseTimings(firstByteSent, lastByteReceived)
    val rawResponse = HttpResponse(request, nettyRequest, remoteAddress, status, headers, body, checksums, bodyLength, resolvedCharset, timings)

    responseTransformer match {
      case None            => rawResponse
      case Some(transformer) => transformer.applyOrElse(rawResponse, ResponseBuilder.Identity)
    }
  }
}

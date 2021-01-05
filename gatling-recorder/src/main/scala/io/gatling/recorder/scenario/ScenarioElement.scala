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

package io.gatling.recorder.scenario

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

import io.gatling.http.client.uri.Uri
import io.gatling.http.fetch.{ ConcurrentResource, HtmlParser }
import io.gatling.http.util.HttpHelper.parseFormBody
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.model._

import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpHeaderNames, HttpHeaderValues, HttpHeaders, HttpUtil }
import io.netty.util.AsciiString
import jodd.net.MimeTypes

private[recorder] final case class TimedScenarioElement[+T <: ScenarioElement](sendTime: Long, arrivalTime: Long, element: T)

private[recorder] sealed trait RequestBody extends Product with Serializable
private[recorder] final case class RequestBodyParams(params: List[(String, String)]) extends RequestBody
@SuppressWarnings(Array("org.wartremover.warts.ArrayEquals"))
private[recorder] final case class RequestBodyBytes(bytes: Array[Byte]) extends RequestBody

private[recorder] sealed trait ResponseBody extends Product with Serializable
@SuppressWarnings(Array("org.wartremover.warts.ArrayEquals"))
private[recorder] final case class ResponseBodyBytes(bytes: Array[Byte]) extends ResponseBody

private[recorder] sealed trait ScenarioElement extends Product with Serializable

private[recorder] final case class PauseElement(duration: FiniteDuration) extends ScenarioElement
private[recorder] final case class TagElement(text: String) extends ScenarioElement

private[recorder] object RequestElement {

  private val CacheHeaders =
    Set(
      HttpHeaderNames.CACHE_CONTROL.toString,
      HttpHeaderNames.IF_MATCH.toString,
      HttpHeaderNames.IF_MODIFIED_SINCE.toString,
      HttpHeaderNames.IF_NONE_MATCH.toString,
      HttpHeaderNames.IF_RANGE.toString,
      HttpHeaderNames.IF_UNMODIFIED_SINCE.toString
    )

  private val HtmlContentType = """(?i)text/html\s*;\s+charset=(.+)?""".r

  def apply(request: HttpRequest, response: HttpResponse)(implicit configuration: RecorderConfiguration): RequestElement = {
    val requestHeaders = request.headers

    val requestBody =
      if (request.body.nonEmpty) {
        val formUrlEncoded =
          Option(requestHeaders.get(HttpHeaderNames.CONTENT_TYPE)).exists(AsciiString.contains(_, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED))
        if (formUrlEncoded)
          // The payload consists of a Unicode string using only characters in the range U+0000 to U+007F
          // cf: http://www.w3.org/TR/html5/forms.html#application/x-www-form-urlencoded-decoding-algorithm
          Some(RequestBodyParams(parseFormBody(new String(request.body, UTF_8.name))))
        else
          Some(RequestBodyBytes(request.body))
      } else {
        None
      }

    val responseBody =
      if (response.body.nonEmpty) {
        Some(ResponseBodyBytes(response.body))
      } else {
        None
      }

    val embeddedResources = Option(response.headers.get(HttpHeaderNames.CONTENT_TYPE))
      .collect {
        case HtmlContentType(headerCharset) if responseBody.nonEmpty =>
          val charset = Option(headerCharset).collect { case charsetName if Charset.isSupported(charsetName) => Charset.forName(charsetName) }.getOrElse(UTF_8)
          val htmlChars = new String(response.body, charset).toCharArray
          new HtmlParser().getEmbeddedResources(Uri.create(request.uri), htmlChars)
      }
      .getOrElse(Nil)

    val filteredRequestHeaders: HttpHeaders =
      if (configuration.http.removeCacheHeaders) {
        val filtered = new DefaultHttpHeaders(false)
        for {
          entry <- requestHeaders.entries.asScala
          if !CacheHeaders.contains(entry.getKey)
        } filtered.add(entry.getKey, entry.getValue)
        filtered
      } else {
        requestHeaders
      }

    RequestElement(request.uri, request.method, filteredRequestHeaders, requestBody, response.headers, responseBody, response.status, embeddedResources, Nil)
  }
}

private[recorder] final case class RequestElement(
    uri: String,
    method: String,
    headers: HttpHeaders,
    body: Option[RequestBody],
    responseHeaders: HttpHeaders,
    responseBody: Option[ResponseBody],
    statusCode: Int,
    embeddedResources: List[ConcurrentResource],
    nonEmbeddedResources: List[RequestElement]
) extends ScenarioElement {

  val (baseUrl, pathQuery) = {
    val uriComponents = Uri.create(uri)

    val base = new StringBuilder().append(uriComponents.getScheme).append("://").append(uriComponents.getHost)
    val port = uriComponents.getScheme match {
      case "http" if !Set(-1, 80).contains(uriComponents.getPort)   => s":${uriComponents.getPort}"
      case "https" if !Set(-1, 443).contains(uriComponents.getPort) => s":${uriComponents.getPort}"
      case _                                                        => ""
    }
    base.append(port)

    (base.toString, uriComponents.toRelativeUrl)
  }
  var printedUrl: String = uri

  // TODO NICO mutable external fields are a very bad idea
  var filteredHeadersId: Option[Int] = None

  var id: Int = 0

  def setId(id: Int): RequestElement = {
    this.id = id
    this
  }

  def makeRelativeTo(baseUrl: String): RequestElement = {
    if (baseUrl == this.baseUrl)
      printedUrl = pathQuery
    this
  }

  val basicAuthCredentials: Option[(String, String)] = {
    def parseCredentials(header: String) =
      new String(Base64.getDecoder.decode(header.split(" ")(1)), UTF_8).split(":") match {
        case Array(username, password) =>
          val credentials = (username, password)
          Some(credentials)
        case _ => None
      }

    Option(headers.get(HttpHeaderNames.AUTHORIZATION)).filter(_.startsWith("Basic ")).flatMap(parseCredentials)
  }

  val (mimeType, responseMimeType) = {
    def getMimeType(headers: HttpHeaders) =
      Option(headers.get(HttpHeaderNames.CONTENT_TYPE))
        .flatMap(e => Option(HttpUtil.getMimeType(e)))
        .getOrElse(HttpHeaderValues.APPLICATION_OCTET_STREAM)
        .toString

    (getMimeType(headers), getMimeType(responseHeaders))
  }

  val (fileExtension, responseFileExtension) = {
    def getFileExtension(mimeType: String) = {
      val extensions = MimeTypes.findExtensionsByMimeTypes(mimeType, false)

      if (extensions.isEmpty) {
        "dat"
      } else {
        extensions(0)
      }
    }

    (getFileExtension(mimeType), getFileExtension(responseMimeType))
  }
}

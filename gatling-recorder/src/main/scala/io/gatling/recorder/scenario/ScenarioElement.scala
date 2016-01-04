/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import scala.collection.breakOut
import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.duration.FiniteDuration
import scala.io.Codec.UTF8

import io.gatling.http.HeaderNames._
import io.gatling.http.HeaderValues._
import io.gatling.http.fetch.{ EmbeddedResource, HtmlParser }
import io.gatling.http.util.HttpHelper.parseFormBody
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.http.model.{SafeHttpRequest, SafeHttpResponse}

import org.asynchttpclient.util.Base64
import org.asynchttpclient.uri.Uri

private[recorder] case class TimedScenarioElement[+T <: ScenarioElement](sendTime: Long, arrivalTime: Long, element: T)

private[recorder] sealed trait RequestBody
private[recorder] case class RequestBodyParams(params: List[(String, String)]) extends RequestBody
private[recorder] case class RequestBodyBytes(bytes: Array[Byte]) extends RequestBody

private[recorder] sealed trait ResponseBody
private[recorder] case class ResponseBodyBytes(bytes: Array[Byte]) extends ResponseBody

private[recorder] sealed trait ScenarioElement

private[recorder] case class PauseElement(duration: FiniteDuration) extends ScenarioElement
private[recorder] case class TagElement(text: String) extends ScenarioElement

private[recorder] object RequestElement {

  val HtmlContentType = """(?i)text/html\s*(;\s+charset=(.+))?""".r

  val CacheHeaders = Set(CacheControl, IfMatch, IfModifiedSince, IfNoneMatch, IfRange, IfUnmodifiedSince)

  def apply(request: SafeHttpRequest, response: SafeHttpResponse)(implicit configuration: RecorderConfiguration): RequestElement = {
    val requestHeaders: Map[String, String] = request.headers.entries.map { entry => (entry.getKey, entry.getValue) }(breakOut)
    val requestContentType = requestHeaders.get(ContentType)
    val requestUserAgent = requestHeaders.get(UserAgent)
    val responseContentType = Option(response.headers.get(ContentType))

    val containsFormParams = requestContentType.exists(_.contains(ApplicationFormUrlEncoded))

    val requestBody =
      if (request.body.nonEmpty) {
        if (containsFormParams)
          // The payload consists of a Unicode string using only characters in the range U+0000 to U+007F
          // cf: http://www.w3.org/TR/html5/forms.html#application/x-www-form-urlencoded-decoding-algorithm
          Some(RequestBodyParams(parseFormBody(new String(request.body, UTF8.name))))
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

    val embeddedResources = responseContentType.collect {
      case HtmlContentType(_, headerCharset) =>
        val charsetName = Option(headerCharset).filter(Charset.isSupported).getOrElse(UTF8.name)
        val charset = Charset.forName(charsetName)
        if (response.body.nonEmpty) {
          val htmlBuff = new String(response.body, charset)
          val userAgent = requestUserAgent.flatMap(io.gatling.http.fetch.UserAgent.parseFromHeader)
          Some(new HtmlParser().getEmbeddedResources(Uri.create(request.uri), htmlBuff, userAgent))
        } else {
          None
        }
    }.flatten.getOrElse(Nil)

    val filteredRequestHeaders =
      if (configuration.http.removeCacheHeaders)
        requestHeaders.filterKeys(name => !CacheHeaders.contains(name))
      else
        requestHeaders

    RequestElement(new String(request.uri), request.method.toString, filteredRequestHeaders, requestBody, responseBody, response.status.code, embeddedResources)
  }
}

private[recorder] case class RequestElement(
    uri:                  String,
    method:               String,
    headers:              Map[String, String],
    body:                 Option[RequestBody],
    responseBody:         Option[ResponseBody],
    statusCode:           Int,
    embeddedResources:    List[EmbeddedResource],
    nonEmbeddedResources: List[RequestElement]   = Nil
) extends ScenarioElement {

  val (baseUrl, pathQuery) = {
    val uriComponents = Uri.create(uri)

    val base = new StringBuilder().append(uriComponents.getScheme).append("://").append(uriComponents.getHost)
    val port = uriComponents.getScheme match {
      case "http" if !Set(-1, 80).contains(uriComponents.getPort) => ":" + uriComponents.getPort
      case "https" if !Set(-1, 443).contains(uriComponents.getPort) => ":" + uriComponents.getPort
      case _ => ""
    }
    base.append(port)

    (base.toString, uriComponents.toRelativeUrl)
  }
  var printedUrl = uri

  // TODO NICO mutable external fields are a very bad idea
  var filteredHeadersId: Option[Int] = None

  var id: Int = 0

  def setId(id: Int) = {
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
        new String(Base64.decode(header.split(" ")(1))).split(":") match {
          case Array(username, password) =>
            val credentials = (username, password)
            Some(credentials)
          case _ => None
        }

    headers.get(Authorization).filter(_.startsWith("Basic ")).flatMap(parseCredentials)
  }
}

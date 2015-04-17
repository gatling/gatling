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

import com.ning.http.client.providers.netty.request.NettyRequest
import com.ning.http.client.uri.Uri
import io.gatling.core.result.message.RequestTimings
import io.gatling.http.config.HttpProtocol

import scala.collection.JavaConversions.asScalaBuffer

import com.ning.http.client.{ FluentCaseInsensitiveStringsMap, HttpResponseStatus, Request => AHCRequest }
import com.ning.http.client.cookie.{ Cookie, CookieDecoder }

import io.gatling.http.HeaderNames
import io.gatling.http.util.HttpHelper

abstract class Response {

  def request: AHCRequest
  def nettyRequest: Option[NettyRequest]
  def remoteAddress: Option[InetAddress]
  def isReceived: Boolean

  def status: Option[HttpResponseStatus]
  def statusCode: Option[Int]
  def uri: Option[Uri]
  def isRedirect: Boolean

  def header(name: String): Option[String]
  def headers: FluentCaseInsensitiveStringsMap
  def headers(name: String): Seq[String]
  def cookies: List[Cookie]

  def checksums: Map[String, String]
  def checksum(algorithm: String): Option[String]
  def hasResponseBody: Boolean
  def body: ResponseBody
  def bodyLength: Int
  def charset: Charset

  def timings: RequestTimings

  def lastModifiedOrEtag(protocol: HttpProtocol): Option[String] =
    if (protocol.requestPart.cache) header(HeaderNames.LastModified).orElse(header(HeaderNames.ETag))
    else None
}

case class HttpResponse(
    request: AHCRequest,
    nettyRequest: Option[NettyRequest],
    remoteAddress: Option[InetAddress],
    status: Option[HttpResponseStatus],
    headers: FluentCaseInsensitiveStringsMap,
    body: ResponseBody,
    checksums: Map[String, String],
    bodyLength: Int,
    charset: Charset,
    timings: RequestTimings) extends Response {

  def isReceived = status.isDefined
  val statusCode = status.map(_.getStatusCode)

  val isRedirect = status match {
    case Some(s) => HttpHelper.isRedirect(s.getStatusCode)
    case _       => false
  }
  def uri = status.map(_.getUri)

  def header(name: String): Option[String] = Option(headers.getFirstValue(name))
  def headers(name: String): Seq[String] = Option(headers.get(name)) match {
    case Some(h) => h.toSeq
    case _       => Nil
  }

  lazy val cookies = headers.get(HeaderNames.SetCookie).flatMap(cookie => Option(CookieDecoder.decode(cookie))).toList

  def checksum(algorithm: String) = checksums.get(algorithm)
  def hasResponseBody = bodyLength != 0
}

class ResponseWrapper(delegate: Response) extends Response {

  def request: AHCRequest = delegate.request
  def nettyRequest: Option[NettyRequest] = delegate.nettyRequest
  def remoteAddress: Option[InetAddress] = delegate.remoteAddress
  def isReceived = delegate.isReceived

  def status = delegate.status
  def statusCode = delegate.statusCode
  def isRedirect = delegate.isRedirect
  def uri = delegate.uri

  def header(name: String) = delegate.header(name)
  def headers = delegate.headers
  def headers(name: String) = delegate.headers(name)
  def cookies = delegate.cookies

  def checksums = delegate.checksums
  def checksum(algorithm: String) = delegate.checksum(algorithm)
  def hasResponseBody = delegate.hasResponseBody
  def body = delegate.body
  def bodyLength = delegate.bodyLength
  def charset = delegate.charset

  def timings = delegate.timings
}

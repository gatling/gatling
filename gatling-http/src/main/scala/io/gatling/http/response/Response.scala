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
package io.gatling.http.response

import java.nio.charset.Charset

import scala.collection.JavaConversions.asScalaBuffer

import io.netty.handler.codec.http.HttpHeaders
import org.asynchttpclient.cookie.{ Cookie, CookieDecoder }
import org.asynchttpclient.netty.request.NettyRequest
import org.asynchttpclient.{ HttpResponseStatus, Request => AHCRequest }
import org.asynchttpclient.uri.Uri

import io.gatling.core.stats.message.ResponseTimings
import io.gatling.http.HeaderNames
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.util.HttpHelper

abstract class Response {

  def request: AHCRequest
  def nettyRequest: Option[NettyRequest]
  def isReceived: Boolean

  def status: Option[HttpResponseStatus]
  def statusCode: Option[Int]
  def uri: Option[Uri]
  def isRedirect: Boolean

  def header(name: String): Option[String]
  def headers: HttpHeaders
  def headers(name: String): Seq[String]
  def cookies: List[Cookie]

  def checksums: Map[String, String]
  def checksum(algorithm: String): Option[String]
  def hasResponseBody: Boolean
  def body: ResponseBody
  def bodyLength: Int
  def charset: Charset

  def timings: ResponseTimings

  def lastModifiedOrEtag(protocol: HttpProtocol): Option[String] =
    if (protocol.requestPart.cache) header(HeaderNames.LastModified).orElse(header(HeaderNames.ETag))
    else None
}

case class HttpResponse(
    request:      AHCRequest,
    nettyRequest: Option[NettyRequest],
    status:       Option[HttpResponseStatus],
    headers:      HttpHeaders,
    body:         ResponseBody,
    checksums:    Map[String, String],
    bodyLength:   Int,
    charset:      Charset,
    timings:      ResponseTimings
) extends Response {

  def isReceived = status.isDefined
  val statusCode = status.map(_.getStatusCode)

  val isRedirect = status match {
    case Some(s) => HttpHelper.isRedirect(s.getStatusCode)
    case _       => false
  }
  def uri = status.map(_.getUri)

  def header(name: String): Option[String] = Option(headers.get(name))
  def headers(name: String): Seq[String] = headers.getAll(name)

  lazy val cookies = headers.getAll(HeaderNames.SetCookie).flatMap(cookie => Option(CookieDecoder.decode(cookie))).toList

  def checksum(algorithm: String) = checksums.get(algorithm)
  def hasResponseBody = bodyLength != 0
}

class ResponseWrapper(delegate: Response) extends Response {

  def request: AHCRequest = delegate.request
  def nettyRequest: Option[NettyRequest] = delegate.nettyRequest
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

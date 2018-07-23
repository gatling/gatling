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

import scala.collection.breakOut
import scala.collection.JavaConverters._

import io.gatling.http.HeaderNames
import io.gatling.http.client.Request
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.util.HttpHelper

import io.netty.handler.codec.http.cookie.{ ClientCookieDecoder, Cookie }
import io.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }

sealed trait HttpResult {
  def request: Request
  def wireRequestHeaders: HttpHeaders
  def startTimestamp: Long
  def endTimestamp: Long
}

case class HttpFailure(
    request:            Request,
    wireRequestHeaders: HttpHeaders,
    startTimestamp:     Long,
    endTimestamp:       Long,
    errorMessage:       String
) extends HttpResult

sealed abstract class Response extends HttpResult {

  def status: HttpResponseStatus
  def isRedirect: Boolean

  def header(name: CharSequence): Option[String]
  def headers: HttpHeaders
  def headers(name: CharSequence): Seq[String]
  def cookies: List[Cookie]

  def checksum(algorithm: String): Option[String]
  def hasResponseBody: Boolean
  def body: ResponseBody
  def bodyLength: Int
  def charset: Charset

  def lastModifiedOrEtag(protocol: HttpProtocol): Option[String] =
    if (protocol.requestPart.cache) header(HeaderNames.LastModified).orElse(header(HeaderNames.ETag))
    else None
}

case class HttpResponse(
    request:            Request,
    wireRequestHeaders: HttpHeaders,
    status:             HttpResponseStatus,
    headers:            HttpHeaders,
    body:               ResponseBody,
    checksums:          Map[String, String],
    bodyLength:         Int,
    charset:            Charset,
    startTimestamp:     Long,
    endTimestamp:       Long
) extends Response {

  override val isRedirect: Boolean = HttpHelper.isRedirect(status)

  override def header(name: CharSequence): Option[String] = Option(headers.get(name))
  override def headers(name: CharSequence): Seq[String] = headers.getAll(name).asScala
  override val cookies: List[Cookie] = {
    val setCookieValues = headers.getAll(HeaderNames.SetCookie)
    if (setCookieValues.isEmpty) {
      Nil
    } else {
      setCookieValues.asScala.flatMap(setCookie => Option(ClientCookieDecoder.LAX.decode(setCookie)))(breakOut)
    }
  }

  override def checksum(algorithm: String): Option[String] = checksums.get(algorithm)
  override def hasResponseBody: Boolean = bodyLength != 0
}

class ResponseWrapper(delegate: Response) extends Response {

  override def request: Request = delegate.request
  override def wireRequestHeaders: HttpHeaders = delegate.wireRequestHeaders

  override def status: HttpResponseStatus = delegate.status
  override def isRedirect: Boolean = delegate.isRedirect

  override def headers: HttpHeaders = delegate.headers
  override def header(name: CharSequence): Option[String] = delegate.header(name)
  override def headers(name: CharSequence): Seq[String] = delegate.headers(name)
  override def cookies: List[Cookie] = delegate.cookies

  override def checksum(algorithm: String): Option[String] = delegate.checksum(algorithm)
  override def hasResponseBody: Boolean = delegate.hasResponseBody
  override def body: ResponseBody = delegate.body
  override def bodyLength: Int = delegate.bodyLength
  override def charset: Charset = delegate.charset

  override def startTimestamp: Long = delegate.startTimestamp
  override def endTimestamp: Long = delegate.endTimestamp
}

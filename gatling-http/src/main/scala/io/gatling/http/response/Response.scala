/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

import scala.collection.JavaConverters._

import io.netty.handler.codec.http.HttpHeaders
import org.asynchttpclient.netty.request.NettyRequest
import org.asynchttpclient.{ HttpResponseStatus, Request => AHCRequest }
import org.asynchttpclient.uri.Uri
import io.gatling.http.HeaderNames
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.util.HttpHelper

import io.netty.handler.codec.http.cookie.{ ClientCookieDecoder, Cookie }

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

  def startTimestamp: Long
  def endTimestamp: Long

  def lastModifiedOrEtag(protocol: HttpProtocol): Option[String] =
    if (protocol.requestPart.cache) header(HeaderNames.LastModified).orElse(header(HeaderNames.ETag))
    else None
}

case class HttpResponse(
    request:        AHCRequest,
    nettyRequest:   Option[NettyRequest],
    status:         Option[HttpResponseStatus],
    headers:        HttpHeaders,
    body:           ResponseBody,
    checksums:      Map[String, String],
    bodyLength:     Int,
    charset:        Charset,
    startTimestamp: Long,
    endTimestamp:   Long
) extends Response {

  override def isReceived: Boolean = status.isDefined
  override val statusCode: Option[Int] = status.map(_.getStatusCode)

  override val isRedirect: Boolean = status match {
    case Some(s) => HttpHelper.isRedirect(s.getStatusCode)
    case _       => false
  }
  override def uri: Option[Uri] = status.map(_.getUri)

  override def header(name: String): Option[String] = Option(headers.get(name))
  override def headers(name: String): Seq[String] = headers.getAll(name).asScala

  override lazy val cookies: List[Cookie] = headers.getAll(HeaderNames.SetCookie).asScala.flatMap(setCookie => Option(ClientCookieDecoder.LAX.decode(setCookie))).toList

  override def checksum(algorithm: String): Option[String] = checksums.get(algorithm)
  override def hasResponseBody: Boolean = bodyLength != 0
}

class ResponseWrapper(delegate: Response) extends Response {

  override def request: AHCRequest = delegate.request
  override def nettyRequest: Option[NettyRequest] = delegate.nettyRequest
  override def isReceived: Boolean = delegate.isReceived

  override def status: Option[HttpResponseStatus] = delegate.status
  override def statusCode: Option[Int] = delegate.statusCode
  override def isRedirect: Boolean = delegate.isRedirect
  override def uri: Option[Uri] = delegate.uri

  override def header(name: String): Option[String] = delegate.header(name)
  override def headers: HttpHeaders = delegate.headers
  override def headers(name: String): Seq[String] = delegate.headers(name)
  override def cookies: List[Cookie] = delegate.cookies

  override def checksums: Map[String, String] = delegate.checksums
  override def checksum(algorithm: String): Option[String] = delegate.checksum(algorithm)
  override def hasResponseBody: Boolean = delegate.hasResponseBody
  override def body: ResponseBody = delegate.body
  override def bodyLength: Int = delegate.bodyLength
  override def charset: Charset = delegate.charset

  override def startTimestamp: Long = delegate.startTimestamp
  override def endTimestamp: Long = delegate.endTimestamp
}

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

import scala.collection.JavaConverters._

import io.gatling.http.HeaderNames
import io.gatling.http.client.Request
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.util.HttpHelper

import io.netty.handler.codec.http.cookie.Cookie
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

case class Response(
    request:            Request,
    wireRequestHeaders: HttpHeaders,
    status:             HttpResponseStatus,
    headers:            HttpHeaders,
    body:               ResponseBody,
    checksums:          Map[String, String],
    bodyLength:         Int,
    charset:            Charset,
    startTimestamp:     Long,
    endTimestamp:       Long,
    isHttp2:            Boolean
) extends HttpResult {

  val isRedirect: Boolean = HttpHelper.isRedirect(status)

  def header(name: CharSequence): Option[String] = Option(headers.get(name))
  def headers(name: CharSequence): Seq[String] = headers.getAll(name).asScala
  val cookies: List[Cookie] = HttpHelper.responseCookies(headers)

  def checksum(algorithm: String): Option[String] = checksums.get(algorithm)
  def hasResponseBody: Boolean = bodyLength != 0

  def lastModifiedOrEtag(protocol: HttpProtocol): Option[String] =
    if (protocol.requestPart.cache) header(HeaderNames.LastModified).orElse(header(HeaderNames.ETag))
    else None
}

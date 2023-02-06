/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

import scala.jdk.CollectionConverters._

import io.gatling.core.check.ChecksumAlgorithm
import io.gatling.http.client.Request
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.util.HttpHelper

import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaders, HttpResponseStatus }
import io.netty.handler.codec.http.cookie.Cookie

sealed trait HttpResult {
  def request: Request
  def startTimestamp: Long
  def endTimestamp: Long
}

final case class HttpFailure(
    request: Request,
    startTimestamp: Long,
    endTimestamp: Long,
    errorMessage: String
) extends HttpResult

final case class Response(
    request: Request,
    startTimestamp: Long,
    endTimestamp: Long,
    status: HttpResponseStatus,
    headers: HttpHeaders,
    body: ResponseBody,
    checksums: Map[ChecksumAlgorithm, String],
    isHttp2: Boolean
) extends HttpResult {
  val isRedirect: Boolean = HttpHelper.isRedirect(status)

  def header(name: CharSequence): Option[String] = Option(headers.get(name))
  def headers(name: CharSequence): Seq[String] = headers.getAll(name).asScala.toSeq
  val cookies: List[Cookie] = HttpHelper.responseCookies(headers)

  def checksum(algorithm: ChecksumAlgorithm): Option[String] = checksums.get(algorithm)

  def lastModifiedOrEtag(protocol: HttpProtocol): Option[String] =
    if (protocol.requestPart.cache) header(HttpHeaderNames.LAST_MODIFIED).orElse(header(HttpHeaderNames.ETAG)) else None
}

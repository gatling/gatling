/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.util

import io.netty.handler.codec.http.HttpHeaderValues._
import io.netty.handler.codec.http.HttpHeaders
import io.netty.util.AsciiString

object HttpUtils {
  val SupportedEncodings = Set(GZIP, DEFLATE)

  def filterSupportedEncodings(acceptEncodingHeaderValue: String): String =
    acceptEncodingHeaderValue
      .split(",")
      .filter(encoding => containsIgnoreCase(SupportedEncodings, encoding.trim))
      .mkString(",")

  def containsIgnoreCase(headers: Iterable[AsciiString], header: String): Boolean =
    headers.exists(_.contentEqualsIgnoreCase(header))

  def getIgnoreCase(httpHeaders: HttpHeaders, header: String): Option[String] =
    Option(httpHeaders.get(header))

  def isHttp2PseudoHeader(header: String): Boolean = header.startsWith(":")
}

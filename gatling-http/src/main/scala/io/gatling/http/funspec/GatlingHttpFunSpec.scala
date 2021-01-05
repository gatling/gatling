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

package io.gatling.http.funspec

import io.gatling.core.Predef._
import io.gatling.core.funspec.GatlingFunSpec
import io.gatling.core.protocol.Protocol
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

abstract class GatlingHttpFunSpec extends GatlingFunSpec {

  /** The base URL to make HTTP requests against */
  def baseUrl: String

  /** HTTP protocol configuration. Use this to add headers and other http configuration. */
  def httpProtocol: HttpProtocolBuilder =
    http
      .baseUrl(baseUrl)
      .acceptHeader("application/json, text/html, text/plain, */*")
      .acceptEncodingHeader("gzip, deflate")

  override def protocolConf: Protocol = httpProtocol

}

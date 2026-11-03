/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.http.config

import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.client.Http2FlowControlWindows
import io.gatling.http.protocol.{ HttpProtocol, HttpProtocolBuilder }

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class HttpProtocolBuilderSpec extends AnyFlatSpecLike with Matchers {
  private val httpProtocolBuilder = HttpProtocolBuilder(GatlingConfiguration.loadForTest())

  "http protocol configuration builder" should "set a silent URI regex" in {
    val builder = httpProtocolBuilder
      .silentUri(".*")

    val config: HttpProtocol = builder.build

    val actualPattern: String = config.requestPart.silentUri.get.toString
    actualPattern.equals(".*") shouldBe true
  }

  it should "use browser-like HTTP/2 flow-control windows by default" in {
    httpProtocolBuilder.build.enginePart.http2FlowControlWindows shouldBe Http2FlowControlWindows.DEFAULT
  }

  it should "set the HTTP/2 stream and connection windows independently" in {
    val windows = httpProtocolBuilder
      .http2StreamWindowSize(1024 * 1024)
      .http2ConnectionWindowSize(10 * 1024 * 1024)
      .build
      .enginePart
      .http2FlowControlWindows

    windows shouldBe new Http2FlowControlWindows(1024 * 1024, 10 * 1024 * 1024)
  }

  it should "reject an HTTP/2 connection window lower than the protocol's initial one" in {
    an[IllegalArgumentException] should be thrownBy httpProtocolBuilder.http2ConnectionWindowSize(1024)
  }
}

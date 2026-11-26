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

package io.gatling.http.check.header

import io.gatling.ValidationValues
import io.gatling.core.{ CoreDsl, EmptySession }
import io.gatling.core.check.{ Check, CheckResult }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HttpDsl
import io.gatling.http.check.body.mockResponse
import io.gatling.http.response.Response

import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpResponseStatus }
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class HttpRawHeadersRegexCheckSpec extends AnyFlatSpecLike with Matchers with ValidationValues with CoreDsl with HttpDsl with EmptySession {
  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private def response(isHttp2: Boolean): Response =
    mockResponse("").copy(
      status = HttpResponseStatus.NOT_FOUND,
      headers = new DefaultHttpHeaders().add("Content-Type", "text/plain").add("X-Foo", "bar").add("X-Foo", "baz"),
      isHttp2 = isHttp2
    )

  "rawHeadersRegex preparer" should "render the status line and one line per header" in {
    HttpRawHeadersRegexCheckMaterializer.RawHeadersPreparer(response(isHttp2 = false)).succeeded shouldBe
      "HTTP/1.1 404 Not Found\nContent-Type: text/plain\nX-Foo: bar\nX-Foo: baz\n"
  }

  it should "render HTTP/2 in the status line for HTTP/2 responses" in {
    HttpRawHeadersRegexCheckMaterializer.RawHeadersPreparer(response(isHttp2 = true)).succeeded should startWith("HTTP/2 404 Not Found\n")
  }

  "rawHeadersRegex.notExists" should "succeed when the pattern is found nowhere" in {
    rawHeadersRegex("test").notExists.check(response(isHttp2 = false), emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)
  }

  it should "fail when the pattern matches a header value" in {
    rawHeadersRegex("ba[rz]").notExists
      .check(response(isHttp2 = false), emptySession, Check.newPreparedCache)
      .failed shouldBe "rawHeadersRegex(ba[rz]).find.notExists, failed"
  }

  "rawHeadersRegex.find" should "match header names and the status line" in {
    rawHeadersRegex("Content-(\\w+)").find.check(response(isHttp2 = false), emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some("Type"),
      None
    )
    rawHeadersRegex("HTTP/1.1 (\\d+)").find.check(response(isHttp2 = false), emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some("404"),
      None
    )
  }

  "rawHeadersRegex.findAll" should "find all occurrences across multi-valued headers" in {
    rawHeadersRegex("(?m)^X-Foo: (.*)$").findAll.check(response(isHttp2 = false), emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some(Seq("bar", "baz")),
      None
    )
  }

  "rawHeadersRegex.ofType" should "capture multiple groups" in {
    rawHeadersRegex("(?m)^(X-Foo): (.*)$")
      .ofType[(String, String)]
      .count
      .check(response(isHttp2 = false), emptySession, Check.newPreparedCache)
      .succeeded shouldBe CheckResult(
      Some(2),
      None
    )
  }
}

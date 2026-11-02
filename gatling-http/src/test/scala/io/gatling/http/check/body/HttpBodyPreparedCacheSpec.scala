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

package io.gatling.http.check.body

import io.gatling.core.{ CoreDsl, EmptySession }
import io.gatling.core.check.Check
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HttpDsl
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.Response

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class HttpBodyPreparedCacheSpec extends AnyFlatSpecLike with Matchers with CoreDsl with HttpDsl with EmptySession {
  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private def preparedCacheSize(response: Response, checks: List[HttpCheck]): Int = {
    val preparedCache = Check.newPreparedCache
    val (_, failure) = Check.applyChecks(response, emptySession, checks, preparedCache)
    failure shouldBe None
    preparedCache.size
  }

  "JSON checks" should "parse the response body only once" in {
    val checks: List[HttpCheck] = List(
      jsonPath("$.foo").is("FOO"),
      jsonPath("$.bar").is("BAR"),
      jmesPath("foo").is("FOO"),
      jmesPath("bar").is("BAR")
    )

    preparedCacheSize(mockResponse("""{"foo": "FOO", "bar": "BAR"}"""), checks) shouldBe 1
  }

  "JSONP checks" should "parse the response body only once" in {
    val checks: List[HttpCheck] = List(
      jsonpJsonPath("$.foo").is("FOO"),
      jsonpJsonPath("$.bar").is("BAR"),
      jsonpJmesPath("foo").is("FOO"),
      jsonpJmesPath("bar").is("BAR")
    )

    preparedCacheSize(mockResponse("""callback({"foo": "FOO", "bar": "BAR"});"""), checks) shouldBe 1
  }

  "CSS checks" should "parse the response body only once" in {
    val checks: List[HttpCheck] = List(
      css("#foo").is("FOO"),
      css("#bar").is("BAR")
    )

    preparedCacheSize(mockResponse("""<html><body><p id="foo">FOO</p><p id="bar">BAR</p></body></html>"""), checks) shouldBe 1
  }
}

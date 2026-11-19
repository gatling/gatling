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

package io.gatling.http.cache

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class ExpiresSupportSpec extends AnyFlatSpecLike with Matchers {
  "parseDeltaSeconds" should "parse non negative integers" in {
    ExpiresSupport.parseDeltaSeconds("0") shouldBe Some(0L)
    ExpiresSupport.parseDeltaSeconds("567") shouldBe Some(567L)
  }

  it should "reject invalid values" in {
    ExpiresSupport.parseDeltaSeconds("") shouldBe None
    ExpiresSupport.parseDeltaSeconds("-1") shouldBe None
    ExpiresSupport.parseDeltaSeconds("nicolas") shouldBe None
    ExpiresSupport.parseDeltaSeconds("1.5") shouldBe None
  }

  it should "cap values to 2^31" in {
    ExpiresSupport.parseDeltaSeconds("2147483649") shouldBe Some(2147483648L)
    ExpiresSupport.parseDeltaSeconds("99999999999999999999") shouldBe Some(2147483648L)
  }

  it should "ignore surrounding spaces" in {
    ExpiresSupport.parseDeltaSeconds(" 60 ") shouldBe Some(60L)
    ExpiresSupport.parseDeltaSeconds("   ") shouldBe None
  }

  it should "ignore leading zeros" in {
    ExpiresSupport.parseDeltaSeconds("00000000060") shouldBe Some(60L)
    ExpiresSupport.parseDeltaSeconds("000") shouldBe Some(0L)
  }

  private def tokens(value: String): List[String] = {
    val tokens = scala.collection.mutable.ListBuffer.empty[String]
    HeaderTokens.exists(value) { (start, end) =>
      tokens += value.substring(start, end)
      false
    }
    tokens.toList
  }

  "HeaderTokens.exists" should "split on commas, trim whitespace and drop empty tokens" in {
    tokens(" a ,\tb,, c ") shouldBe List("a", "b", "c")
    tokens("") shouldBe Nil
  }

  it should "ignore commas in quoted strings" in {
    tokens("a=\"b, c\", d") shouldBe List("a=\"b, c\"", "d")
    tokens("a=\"b\\\", c\", d") shouldBe List("a=\"b\\\", c\"", "d")
  }

  it should "keep the last token of malformed values" in {
    tokens("a, b=\"c, d") shouldBe List("a", "b=\"c, d")
    tokens("a, b=\"c\\") shouldBe List("a", "b=\"c\\")
  }

  it should "stop as soon as a token matches" in {
    var seen = 0
    HeaderTokens.exists("a, b, c") { (start, _) =>
      seen += 1
      start == 3
    } shouldBe true
    seen shouldBe 2
  }

  "CacheControl.parse" should "parse directives case-insensitively" in {
    CacheControl.parse("Public, MAX-AGE=60, No-Cache, must-revalidate, no-store") shouldBe CacheControl(
      noStore = true,
      noCache = true,
      mustRevalidate = true,
      maxAge = Some(60)
    )
  }

  it should "only match exact no-cache and no-store, like Chrome" in {
    CacheControl
      .parse("no-cache=\"Set-Cookie\", no-store=\"foo\"") shouldBe CacheControl(noStore = false, noCache = false, mustRevalidate = false, maxAge = None)
  }

  it should "keep the first valid max-age, like Chrome" in {
    CacheControl.parse("max-age=60, max-age=10").maxAge shouldBe Some(60)
    CacheControl.parse("max-age=nicolas, max-age=10").maxAge shouldBe Some(10)
    CacheControl.parse("max-age=\"60\"").maxAge shouldBe None
    CacheControl.parse("max-age = 60").maxAge shouldBe None
  }

  "parseHttpDate" should "support the HTTP date format" in {
    ExpiresSupport.parseHttpDate("Thu, 01 Dec 1994 16:00:00 GMT") shouldBe Some(786297600000L)
    ExpiresSupport.parseHttpDate("\"Tue, 19 Jan 2038 03:14:06 GMT\"") shouldBe Some(2147483646000L)
  }

  it should "reject invalid dates" in {
    ExpiresSupport.parseHttpDate("0") shouldBe None
    ExpiresSupport.parseHttpDate("fail") shouldBe None
  }
}

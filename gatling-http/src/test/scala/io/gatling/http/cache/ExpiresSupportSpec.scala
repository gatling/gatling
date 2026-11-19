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

  "parseCacheControl" should "parse directives, case-insensitively" in {
    ExpiresSupport.parseCacheControl("Public, MAX-AGE=60, no-cache=\"Set-Cookie\"") shouldBe Map(
      "public" -> None,
      "max-age" -> Some("60"),
      "no-cache" -> Some("Set-Cookie")
    )
  }

  it should "keep the first occurrence of a repeated directive" in {
    ExpiresSupport.parseCacheControl("max-age=60, max-age=10") shouldBe Map("max-age" -> Some("60"))
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

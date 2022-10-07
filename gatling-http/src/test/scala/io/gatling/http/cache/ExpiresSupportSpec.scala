/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import io.gatling.BaseSpec

class ExpiresSupportSpec extends BaseSpec {
  "extractLongValue" should "throw an IllegalArgumentException if start < 0 or > the string length " in {
    an[IllegalArgumentException] should be thrownBy ExpiresSupport.extractLongValue("1234", -1)
    an[IllegalArgumentException] should be thrownBy ExpiresSupport.extractLongValue("1234", 4)
  }

  it should "be able to extract a Long from a series of digits in a string" in {
    ExpiresSupport.extractLongValue("foo12345bar", 3) shouldBe 12345L
  }

  it should "return 0 if there was no series of digit at the specified index" in {
    ExpiresSupport.extractLongValue("foobar", 3) shouldBe 0L
  }
}

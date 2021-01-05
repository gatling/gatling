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

package io.gatling.commons.util

import io.gatling.BaseSpec
import io.gatling.commons.util.NumberHelper._

class NumberHelperSpec extends BaseSpec {

  "extractLongValue" should "throw an IllegalArgumentException if start < 0 or > the string length " in {
    an[IllegalArgumentException] should be thrownBy extractLongValue("1234", -1)
    an[IllegalArgumentException] should be thrownBy extractLongValue("1234", 4)
  }

  it should "be able to extract a Long from a series of digits in a string" in {
    extractLongValue("foo12345bar", 3) shouldBe 12345L
  }

  it should "return 0 if there was no series of digit at the specified index" in {
    extractLongValue("foobar", 3) shouldBe 0L
  }

  "toRank" should "return '1st' for 1" in {
    1.toRank shouldBe "1st"
  }

  it should "return '2nd' for 2" in {
    2.toRank shouldBe "2nd"
  }

  it should "return '3rd' for 3" in {
    3.toRank shouldBe "3rd"
  }

  it should "return '4th' for 4" in {
    4.toRank shouldBe "4th"
  }

  it should "return '11th' for 11" in {
    11.toRank shouldBe "11th"
  }

  it should "return '12th' for 12" in {
    12.toRank shouldBe "12th"
  }

  it should "return '13th' for 13" in {
    13.toRank shouldBe "13th"
  }

  it should "return '21st' for 21" in {
    21.toRank shouldBe "21st"
  }

  it should "return '12341st' for 12341" in {
    12341.toRank shouldBe "12341st"
  }

  it should "return '12311th' for 12311" in {
    12311.toRank shouldBe "12311th"
  }

  it should "return '1st' for 1.0" in {
    1.0.toRank shouldBe "1st"
  }

  it should "return '2nd' for 2.0" in {
    2.0.toRank shouldBe "2nd"
  }

  it should "return '3rd' for 3.0" in {
    3.0.toRank shouldBe "3rd"
  }

  it should "return '4th' for 4.0" in {
    4.0.toRank shouldBe "4th"
  }

  it should "return '11th' for 11.0" in {
    11.0.toRank shouldBe "11th"
  }

  it should "return '12th' for 12.0" in {
    12.0.toRank shouldBe "12th"
  }

  it should "return '13th' for 13.0" in {
    13.0.toRank shouldBe "13th"
  }

  it should "return '21st' for 21.0" in {
    21.0.toRank shouldBe "21st"
  }

  it should "return '99th' for 99.0" in {
    99.0.toRank shouldBe "99th"
  }

  it should "return '99.8th' for 99.8" in {
    99.8.toRank shouldBe "99.8th"
  }

  it should "return '99.99th' for 99.99" in {
    99.99.toRank shouldBe "99.99th"
  }

  it should "return '4.01st' for 4.01" in {
    4.01.toRank shouldBe "4.01st"
  }
}

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

package io.gatling.core.util

import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class RandomDistributionSpec extends AnyFlatSpec with Matchers with MockitoSugar with GivenWhenThen {

  "uniform" should "work" in {
    val distribution = RandomDistribution.uniform(List("a", "b", "c", "d"))

    distribution.next(0) shouldBe "a"
    distribution.next(1) shouldBe "b"
    distribution.next(2) shouldBe "c"
    distribution.next(3) shouldBe "d"
  }

  "percentWeights" should "support whole number weights" in {
    val distribution = RandomDistribution.percentWeights(List(25.0 -> "a", 25.0 -> "b", 25.0 -> "c", 25.0 -> "d"), "FALLBACK")

    distribution.next(0) shouldBe "a"
    distribution.next(24999999) shouldBe "a"
    distribution.next(25000000) shouldBe "b"
    distribution.next(49999999) shouldBe "b"
    distribution.next(50000000) shouldBe "c"
    distribution.next(74999999) shouldBe "c"
    distribution.next(75000000) shouldBe "d"
    distribution.next(99999999) shouldBe "d"
  }

  it should "support double weights" in {
    val oneThird = 100.0 / 3
    val distribution = RandomDistribution.percentWeights(List(oneThird -> "a", oneThird -> "b", oneThird -> "c"), "FALLBACK")

    distribution.next(0) shouldBe "a"
    distribution.next(33333333) shouldBe "a"
    distribution.next(33333334) shouldBe "b"
    distribution.next(66666666) shouldBe "b"
    distribution.next(66666667) shouldBe "c"
    distribution.next(99999999) shouldBe "c"
  }

  it should "use fallback if sum is not 100" in {
    val distribution = RandomDistribution.percentWeights(List(30.0 -> "a", 50.0 -> "b", 19.999 -> "c"), "FALLBACK")

    distribution.next(0) shouldBe "a"
    distribution.next(29999999) shouldBe "a"
    distribution.next(30000000) shouldBe "b"
    distribution.next(79999999) shouldBe "b"
    distribution.next(80000000) shouldBe "c"
    distribution.next(99998999) shouldBe "c"
    distribution.next(99999000) shouldBe "FALLBACK"
    distribution.next(99999999) shouldBe "FALLBACK"
  }
}

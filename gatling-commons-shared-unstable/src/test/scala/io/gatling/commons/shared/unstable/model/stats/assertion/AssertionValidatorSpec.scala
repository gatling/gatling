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

package io.gatling.commons.shared.unstable.model.stats.assertion

import io.gatling.commons.shared.unstable.model.stats.{ GeneralStats, GeneralStatsSource }
import io.gatling.commons.stats.assertion.{ Assertion, Between, Global, MeanRequestsPerSecondTarget }

import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class AssertionValidatorSpec extends AnyFlatSpecLike with Matchers with MockitoSugar {

  private def validateAssertions[T](
      assertions: List[Assertion],
      mockedMethodCall: GeneralStatsSource => T,
      mockedMethodCallResult: T
  ): List[AssertionResult] = {
    val source = mock[GeneralStatsSource]
    when(source.assertions) thenReturn assertions
    when(mockedMethodCall(source)) thenReturn mockedMethodCallResult
    AssertionValidator.validateAssertions(source)
  }

  "global.requestsPerSec.between" should "return a success when actual value is within inclusive" in {

    val res = validateAssertions(
      List(Assertion(path = Global, target = MeanRequestsPerSecondTarget, condition = Between(0.0, 1.0, inclusive = true))),
      _.requestGeneralStats(None, None, None),
      GeneralStats.NoPlot.copy(meanRequestsPerSec = 0.1)
    )

    res.size shouldBe 1
    res.head.result shouldBe true
  }

  it should "return a success when actual value is inclusive range higher bound" in {

    val res = validateAssertions(
      List(Assertion(path = Global, target = MeanRequestsPerSecondTarget, condition = Between(0.0, 1.0, inclusive = true))),
      _.requestGeneralStats(None, None, None),
      GeneralStats.NoPlot.copy(meanRequestsPerSec = 1.0)
    )

    res.size shouldBe 1
    res.head.result shouldBe true
  }

  it should "return a success when actual value is inclusive range lower bound" in {

    val res = validateAssertions(
      List(Assertion(path = Global, target = MeanRequestsPerSecondTarget, condition = Between(0.0, 1.0, inclusive = true))),
      _.requestGeneralStats(None, None, None),
      GeneralStats.NoPlot.copy(meanRequestsPerSec = 0.0)
    )

    res.size shouldBe 1
    res.head.result shouldBe true
  }

  it should "return a failure when actual value is outside inclusive range" in {

    val res = validateAssertions(
      List(Assertion(path = Global, target = MeanRequestsPerSecondTarget, condition = Between(0.0, 1.0, inclusive = true))),
      _.requestGeneralStats(None, None, None),
      GeneralStats.NoPlot.copy(meanRequestsPerSec = 1.1)
    )

    res.size shouldBe 1
    res.head.result shouldBe false
  }

  it should "return a success when actual value is within exclusive range" in {

    val res = validateAssertions(
      List(Assertion(path = Global, target = MeanRequestsPerSecondTarget, condition = Between(0.0, 1.0, inclusive = false))),
      _.requestGeneralStats(None, None, None),
      GeneralStats.NoPlot.copy(meanRequestsPerSec = 0.1)
    )

    res.size shouldBe 1
    res.head.result shouldBe true
  }

  it should "return a failure when actual value is exclusive range higher bound" in {

    val res = validateAssertions(
      List(Assertion(path = Global, target = MeanRequestsPerSecondTarget, condition = Between(0.0, 1.0, inclusive = false))),
      _.requestGeneralStats(None, None, None),
      GeneralStats.NoPlot.copy(meanRequestsPerSec = 1.0)
    )

    res.size shouldBe 1
    res.head.result shouldBe false
  }

  it should "return a failure when actual value is exclusive range lower bound" in {

    val res = validateAssertions(
      List(Assertion(path = Global, target = MeanRequestsPerSecondTarget, condition = Between(0.0, 1.0, inclusive = false))),
      _.requestGeneralStats(None, None, None),
      GeneralStats.NoPlot.copy(meanRequestsPerSec = 0.0)
    )

    res.size shouldBe 1
    res.head.result shouldBe false
  }

  it should "return a failure when actual value is outside exclusive range" in {

    val res = validateAssertions(
      List(Assertion(path = Global, target = MeanRequestsPerSecondTarget, condition = Between(0.0, 1.0, inclusive = false))),
      _.requestGeneralStats(None, None, None),
      GeneralStats.NoPlot.copy(meanRequestsPerSec = 1.1)
    )

    res.size shouldBe 1
    res.head.result shouldBe false
  }
}

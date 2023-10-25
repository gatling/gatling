/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.stats.assertion._

import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class AssertionValidatorSpec extends AnyFlatSpecLike with Matchers with MockitoSugar {
  private def validateAssertions(
      assertions: List[Assertion],
      mockedMethodCall: AssertionStatsRepository => AssertionStatsRepository.Stats,
      mockedMethodCallResult: AssertionStatsRepository.Stats
  ): List[AssertionResult] = {
    val repository = mock[AssertionStatsRepository]
    when(mockedMethodCall(repository)) thenReturn mockedMethodCallResult
    new AssertionValidator(repository).validateAssertions(assertions)
  }

  "global.requestsPerSec.between" should "return a success when actual value is within inclusive" in {
    val res = validateAssertions(
      List(Assertion(path = AssertionPath.Global, target = Target.MeanRequestsPerSecond, condition = Condition.Between(0.0, 1.0, inclusive = true))),
      _.requestGeneralStats(Nil, None, None),
      AssertionStatsRepository.Stats.NoData.copy(meanRequestsPerSec = 0.1)
    )

    res.size shouldBe 1
    res.head.success shouldBe true
  }

  it should "return a success when actual value is inclusive range higher bound" in {
    val res = validateAssertions(
      List(Assertion(path = AssertionPath.Global, target = Target.MeanRequestsPerSecond, condition = Condition.Between(0.0, 1.0, inclusive = true))),
      _.requestGeneralStats(Nil, None, None),
      AssertionStatsRepository.Stats.NoData.copy(meanRequestsPerSec = 1.0)
    )

    res.size shouldBe 1
    res.head.success shouldBe true
  }

  it should "return a success when actual value is inclusive range lower bound" in {
    val res = validateAssertions(
      List(Assertion(path = AssertionPath.Global, target = Target.MeanRequestsPerSecond, condition = Condition.Between(0.0, 1.0, inclusive = true))),
      _.requestGeneralStats(Nil, None, None),
      AssertionStatsRepository.Stats.NoData.copy(meanRequestsPerSec = 0.0)
    )

    res.size shouldBe 1
    res.head.success shouldBe true
  }

  it should "return a failure when actual value is outside inclusive range" in {
    val res = validateAssertions(
      List(Assertion(path = AssertionPath.Global, target = Target.MeanRequestsPerSecond, condition = Condition.Between(0.0, 1.0, inclusive = true))),
      _.requestGeneralStats(Nil, None, None),
      AssertionStatsRepository.Stats.NoData.copy(meanRequestsPerSec = 1.1)
    )

    res.size shouldBe 1
    res.head.success shouldBe false
  }

  it should "return a success when actual value is within exclusive range" in {
    val res = validateAssertions(
      List(Assertion(path = AssertionPath.Global, target = Target.MeanRequestsPerSecond, condition = Condition.Between(0.0, 1.0, inclusive = false))),
      _.requestGeneralStats(Nil, None, None),
      AssertionStatsRepository.Stats.NoData.copy(meanRequestsPerSec = 0.1)
    )

    res.size shouldBe 1
    res.head.success shouldBe true
  }

  it should "return a failure when actual value is exclusive range higher bound" in {
    val res = validateAssertions(
      List(Assertion(path = AssertionPath.Global, target = Target.MeanRequestsPerSecond, condition = Condition.Between(0.0, 1.0, inclusive = false))),
      _.requestGeneralStats(Nil, None, None),
      AssertionStatsRepository.Stats.NoData.copy(meanRequestsPerSec = 1.0)
    )

    res.size shouldBe 1
    res.head.success shouldBe false
  }

  it should "return a failure when actual value is exclusive range lower bound" in {
    val res = validateAssertions(
      List(Assertion(path = AssertionPath.Global, target = Target.MeanRequestsPerSecond, condition = Condition.Between(0.0, 1.0, inclusive = false))),
      _.requestGeneralStats(Nil, None, None),
      AssertionStatsRepository.Stats.NoData.copy(meanRequestsPerSec = 0.0)
    )

    res.size shouldBe 1
    res.head.success shouldBe false
  }

  it should "return a failure when actual value is outside exclusive range" in {
    val res = validateAssertions(
      List(Assertion(path = AssertionPath.Global, target = Target.MeanRequestsPerSecond, condition = Condition.Between(0.0, 1.0, inclusive = false))),
      _.requestGeneralStats(Nil, None, None),
      AssertionStatsRepository.Stats.NoData.copy(meanRequestsPerSec = 1.1)
    )

    res.size shouldBe 1
    res.head.success shouldBe false
  }
}

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

package io.gatling.core.assertion

import io.gatling.BaseSpec
import io.gatling.core.config.GatlingConfiguration
import io.gatling.shared.model.assertion._

class AssertionDSLSpec extends BaseSpec with AssertionSupport {
  "The Assertion DSL builders" should "produce the expected Assertions ASTs" in {
    implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

    global.responseTime.min.is(100) shouldBe Assertion(AssertionPath.Global, Target.Time(TimeMetric.ResponseTime, Stat.Min), Condition.Is(100))
    details("Foo" / "Bar").responseTime.max
      .lte(100) shouldBe Assertion(AssertionPath.Details(List("Foo", "Bar")), Target.Time(TimeMetric.ResponseTime, Stat.Max), Condition.Lte(100))
    forAll.responseTime.mean.gte(100) shouldBe Assertion(AssertionPath.ForAll, Target.Time(TimeMetric.ResponseTime, Stat.Mean), Condition.Gte(100))
    global.responseTime.stdDev.between(1, 3) shouldBe Assertion(
      AssertionPath.Global,
      Target.Time(TimeMetric.ResponseTime, Stat.StandardDeviation),
      Condition.Between(1, 3, inclusive = true)
    )
    global.responseTime.percentile1
      .is(300) shouldBe Assertion(AssertionPath.Global, Target.Time(TimeMetric.ResponseTime, Stat.Percentile(50)), Condition.Is(300))
    global.responseTime.percentile2
      .in(Set(1, 2, 3)) shouldBe Assertion(AssertionPath.Global, Target.Time(TimeMetric.ResponseTime, Stat.Percentile(75)), Condition.In(List(1, 2, 3)))
    global.responseTime.percentile3
      .is(300) shouldBe Assertion(AssertionPath.Global, Target.Time(TimeMetric.ResponseTime, Stat.Percentile(95)), Condition.Is(300))
    global.responseTime.percentile4
      .in(Set(1, 2, 3)) shouldBe Assertion(AssertionPath.Global, Target.Time(TimeMetric.ResponseTime, Stat.Percentile(99)), Condition.In(List(1, 2, 3)))

    global.allRequests.count.is(20) shouldBe Assertion(AssertionPath.Global, Target.Count(CountMetric.AllRequests), Condition.Is(20))
    forAll.allRequests.percent.lt(5) shouldBe Assertion(AssertionPath.ForAll, Target.Percent(CountMetric.AllRequests), Condition.Lt(5))

    global.failedRequests.count.gt(10) shouldBe Assertion(AssertionPath.Global, Target.Count(CountMetric.FailedRequests), Condition.Gt(10))
    details("Foo" / "Bar").failedRequests.percent.between(1, 5, inclusive = false) shouldBe Assertion(
      AssertionPath.Details(List("Foo", "Bar")),
      Target.Percent(CountMetric.FailedRequests),
      Condition.Between(1, 5, inclusive = false)
    )

    global.successfulRequests.count.in(1, 2, 2, 4) shouldBe Assertion(
      AssertionPath.Global,
      Target.Count(CountMetric.SuccessfulRequests),
      Condition.In(List(1, 2, 4))
    )
    global.successfulRequests.percent.is(6) shouldBe Assertion(AssertionPath.Global, Target.Percent(CountMetric.SuccessfulRequests), Condition.Is(6))

    global.requestsPerSec.is(35) shouldBe Assertion(AssertionPath.Global, Target.MeanRequestsPerSecond, Condition.Is(35))
    global.requestsPerSec.around(35, 3) shouldBe Assertion(AssertionPath.Global, Target.MeanRequestsPerSecond, Condition.Between(32, 38, inclusive = true))
  }
}

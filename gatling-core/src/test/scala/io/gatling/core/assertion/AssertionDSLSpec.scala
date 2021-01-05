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

package io.gatling.core.assertion

import io.gatling.BaseSpec
import io.gatling.commons.stats.assertion._
import io.gatling.core.config.GatlingConfiguration

class AssertionDSLSpec extends BaseSpec with AssertionSupport {

  "The Assertion DSL builders" should "produce the expected Assertions ASTs" in {

    implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

    global.responseTime.min.is(100) shouldBe Assertion(Global, TimeTarget(ResponseTime, Min), Is(100))
    details("Foo" / "Bar").responseTime.max.lte(100) shouldBe Assertion(Details(List("Foo", "Bar")), TimeTarget(ResponseTime, Max), Lte(100))
    forAll.responseTime.mean.gte(100) shouldBe Assertion(ForAll, TimeTarget(ResponseTime, Mean), Gte(100))
    global.responseTime.stdDev.between(1, 3) shouldBe Assertion(Global, TimeTarget(ResponseTime, StandardDeviation), Between(1, 3, inclusive = true))
    global.responseTime.percentile1.is(300) shouldBe Assertion(Global, TimeTarget(ResponseTime, Percentiles(50)), Is(300))
    global.responseTime.percentile2.in(Set(1, 2, 3)) shouldBe Assertion(Global, TimeTarget(ResponseTime, Percentiles(75)), In(List(1, 2, 3)))
    global.responseTime.percentile3.is(300) shouldBe Assertion(Global, TimeTarget(ResponseTime, Percentiles(95)), Is(300))
    global.responseTime.percentile4.in(Set(1, 2, 3)) shouldBe Assertion(Global, TimeTarget(ResponseTime, Percentiles(99)), In(List(1, 2, 3)))

    global.allRequests.count.is(20) shouldBe Assertion(Global, CountTarget(AllRequests), Is(20))
    forAll.allRequests.percent.lt(5) shouldBe Assertion(ForAll, PercentTarget(AllRequests), Lt(5))

    global.failedRequests.count.gt(10) shouldBe Assertion(Global, CountTarget(FailedRequests), Gt(10))
    details("Foo" / "Bar").failedRequests.percent.between(1, 5, inclusive = false) shouldBe Assertion(
      Details(List("Foo", "Bar")),
      PercentTarget(FailedRequests),
      Between(1, 5, inclusive = false)
    )

    global.successfulRequests.count.in(1, 2, 2, 4) shouldBe Assertion(Global, CountTarget(SuccessfulRequests), In(List(1, 2, 4)))
    global.successfulRequests.percent.is(6) shouldBe Assertion(Global, PercentTarget(SuccessfulRequests), Is(6))

    global.requestsPerSec.is(35) shouldBe Assertion(Global, MeanRequestsPerSecondTarget, Is(35))
    global.requestsPerSec.around(35, 3) shouldBe Assertion(Global, MeanRequestsPerSecondTarget, Between(32, 38, inclusive = true))
  }
}

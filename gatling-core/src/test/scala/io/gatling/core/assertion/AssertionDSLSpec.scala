/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.assertion

import io.gatling.BaseSpec

class AssertionDSLSpec extends BaseSpec with AssertionSupport {

  "The Assertion DSL builders" should "produce the expected Assertions ASTs" in {

    global.responseTime.min.is(100) shouldBe Assertion(Global, TimeTarget(ResponseTime, Min), Is(100))
    details("Foo" / "Bar").responseTime.max.lessThan(100) shouldBe Assertion(Details(List("Foo", "Bar")), TimeTarget(ResponseTime, Max), LessThan(100))
    forAll.responseTime.mean.greaterThan(100) shouldBe Assertion(ForAll, TimeTarget(ResponseTime, Mean), GreaterThan(100))
    global.responseTime.stdDev.between(1, 3) shouldBe Assertion(Global, TimeTarget(ResponseTime, StandardDeviation), Between(1, 3))
    global.responseTime.percentile1.is(300) shouldBe Assertion(Global, TimeTarget(ResponseTime, Percentiles1), Is(300))
    global.responseTime.percentile2.in(Set(1, 2, 3)) shouldBe Assertion(Global, TimeTarget(ResponseTime, Percentiles2), In(List(1, 2, 3)))
    global.responseTime.percentile3.is(300) shouldBe Assertion(Global, TimeTarget(ResponseTime, Percentiles3), Is(300))
    global.responseTime.percentile4.in(Set(1, 2, 3)) shouldBe Assertion(Global, TimeTarget(ResponseTime, Percentiles4), In(List(1, 2, 3)))

    global.allRequests.count.is(20) shouldBe Assertion(Global, CountTarget(AllRequests, Count), Is(20))
    forAll.allRequests.percent.lessThan(5) shouldBe Assertion(ForAll, CountTarget(AllRequests, Percent), LessThan(5))

    global.failedRequests.count.greaterThan(10) shouldBe Assertion(Global, CountTarget(FailedRequests, Count), GreaterThan(10))
    details("Foo" / "Bar").failedRequests.percent.between(1, 5) shouldBe Assertion(Details(List("Foo", "Bar")), CountTarget(FailedRequests, Percent), Between(1, 5))

    global.successfulRequests.count.in(1, 2, 2, 4) shouldBe Assertion(Global, CountTarget(SuccessfulRequests, Count), In(List(1, 2, 4)))
    global.successfulRequests.percent.is(6) shouldBe Assertion(Global, CountTarget(SuccessfulRequests, Percent), Is(6))

    global.requestsPerSec.is(35) shouldBe Assertion(Global, MeanRequestsPerSecondTarget, Is(35))

  }
}

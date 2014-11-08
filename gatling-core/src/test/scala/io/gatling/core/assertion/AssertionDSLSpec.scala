package io.gatling.core.assertion

import org.scalatest.{ FlatSpec, Matchers }

import io.gatling.core.assertion._

class AssertionDSLSpec extends FlatSpec with Matchers with AssertionSupport {

  "The Assertion DSL builders" should "produce the expected Assertions ASTs" in {

    global.responseTime.min.is(100) shouldBe Assertion(Global, TimeTarget(ResponseTime, Min), Is(100))
    details("Foo" / "Bar").responseTime.max.lessThan(100) shouldBe Assertion(Details(List("Foo", "Bar")), TimeTarget(ResponseTime, Max), LessThan(100))
    forAll.responseTime.mean.greaterThan(100) shouldBe Assertion(ForAll, TimeTarget(ResponseTime, Mean), GreaterThan(100))
    global.responseTime.stdDev.between(1, 3) shouldBe Assertion(Global, TimeTarget(ResponseTime, StandardDeviation), Between(1, 3))
    global.responseTime.percentile1.is(300) shouldBe Assertion(Global, TimeTarget(ResponseTime, Percentiles1), Is(300))
    global.responseTime.percentile2.in(Set(1, 2, 3)) shouldBe Assertion(Global, TimeTarget(ResponseTime, Percentiles2), In(List(1, 2, 3)))

    global.allRequests.count.is(20) shouldBe Assertion(Global, CountTarget(AllRequests, Count), Is(20))
    forAll.allRequests.percent.lessThan(5) shouldBe Assertion(ForAll, CountTarget(AllRequests, Percent), LessThan(5))

    global.failedRequests.count.greaterThan(10) shouldBe Assertion(Global, CountTarget(FailedRequests, Count), GreaterThan(10))
    details("Foo" / "Bar").failedRequests.percent.between(1, 5) shouldBe Assertion(Details(List("Foo", "Bar")), CountTarget(FailedRequests, Percent), Between(1, 5))

    global.successfulRequests.count.in(Set(1, 2, 4)) shouldBe Assertion(Global, CountTarget(SuccessfulRequests, Count), In(List(1, 2, 4)))
    global.successfulRequests.percent.is(6) shouldBe Assertion(Global, CountTarget(SuccessfulRequests, Percent), Is(6))
  }
}

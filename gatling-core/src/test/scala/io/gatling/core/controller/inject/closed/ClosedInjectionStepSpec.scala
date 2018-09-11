package io.gatling.core.controller.inject.closed

import io.gatling.BaseSpec

import scala.concurrent.duration._

class ClosedInjectionStepSpec extends BaseSpec {

  "ConstantConcurrentNumberInjection" should "return the correct number of users target" in {
    val step = ConstantConcurrentNumberInjection(5, 2 second)
    step.valueAt(0 seconds) shouldBe 5
    step.valueAt(1 seconds) shouldBe 5
    step.valueAt(2 seconds) shouldBe 5
  }

  "RampConcurrentNumberInjection" should "return the correct number of users target" in {
    val step = RampConcurrentNumberInjection(5, 10, 5 second)
    step.valueAt(0 seconds) shouldBe 5
    step.valueAt(1 seconds) shouldBe 6
    step.valueAt(2 seconds) shouldBe 7
    step.valueAt(3 seconds) shouldBe 8
    step.valueAt(4 seconds) shouldBe 9
    step.valueAt(5 seconds) shouldBe 10
  }
}

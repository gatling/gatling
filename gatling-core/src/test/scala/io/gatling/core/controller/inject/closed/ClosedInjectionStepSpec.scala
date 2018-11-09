/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.core.controller.inject.closed

import io.gatling.BaseSpec

import scala.concurrent.duration._

class ClosedInjectionStepSpec extends BaseSpec with MetaClosedInjectionSupport {

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

  "getInjectionSteps" should "produce the expected injection profile with ramps and starting users" in {
    val steps = IncreasingConcurrentUsersProfile(
      concurrentUsers = 10,
      nbOfSteps = 5,
      duration = 10 seconds,
      startingUsers = 5,
      rampDuration = 20 seconds
    ).getInjectionSteps.toSeq

    val expected = Seq(
      ConstantConcurrentNumberInjection(5, 10 seconds), RampConcurrentNumberInjection(5, 15, 20 seconds),
      ConstantConcurrentNumberInjection(15, 10 seconds), RampConcurrentNumberInjection(15, 25, 20 seconds),
      ConstantConcurrentNumberInjection(25, 10 seconds), RampConcurrentNumberInjection(25, 35, 20 seconds),
      ConstantConcurrentNumberInjection(35, 10 seconds), RampConcurrentNumberInjection(35, 45, 20 seconds),
      ConstantConcurrentNumberInjection(45, 10 seconds)
    )

    steps.shouldBe(expected)
  }

  it should "produce the expected injection profile without starting users and ramp" in {
    val steps = IncreasingConcurrentUsersProfile(
      concurrentUsers = 10,
      nbOfSteps = 5,
      duration = 10 seconds,
      startingUsers = 0,
      rampDuration = Duration.Zero
    ).getInjectionSteps.toSeq

    val expected = Seq(
      ConstantConcurrentNumberInjection(10, 10 seconds),
      ConstantConcurrentNumberInjection(20, 10 seconds),
      ConstantConcurrentNumberInjection(30, 10 seconds),
      ConstantConcurrentNumberInjection(40, 10 seconds),
      ConstantConcurrentNumberInjection(50, 10 seconds)
    )

    steps.shouldBe(expected)
  }

  it should "produce the expected injection profile with starting users and without ramp" in {
    val steps = IncreasingConcurrentUsersProfile(
      concurrentUsers = 10,
      nbOfSteps = 5,
      duration = 10 seconds,
      startingUsers = 5,
      rampDuration = Duration.Zero
    ).getInjectionSteps.toSeq

    val expected = Seq(
      ConstantConcurrentNumberInjection(5, 10 seconds),
      ConstantConcurrentNumberInjection(15, 10 seconds),
      ConstantConcurrentNumberInjection(25, 10 seconds),
      ConstantConcurrentNumberInjection(35, 10 seconds),
      ConstantConcurrentNumberInjection(45, 10 seconds)
    )

    steps.shouldBe(expected)
  }

  it should "produce the expected injection profile without starting users and with ramps" in {
    val steps = IncreasingConcurrentUsersProfile(
      concurrentUsers = 10,
      nbOfSteps = 5,
      duration = 10 seconds,
      startingUsers = 0,
      rampDuration = 80 seconds
    ).getInjectionSteps.toSeq

    val expected = Seq(
      ConstantConcurrentNumberInjection(10, 10 seconds), RampConcurrentNumberInjection(10, 20, 80 seconds),
      ConstantConcurrentNumberInjection(20, 10 seconds), RampConcurrentNumberInjection(20, 30, 80 seconds),
      ConstantConcurrentNumberInjection(30, 10 seconds), RampConcurrentNumberInjection(30, 40, 80 seconds),
      ConstantConcurrentNumberInjection(40, 10 seconds), RampConcurrentNumberInjection(40, 50, 80 seconds),
      ConstantConcurrentNumberInjection(50, 10 seconds)
    )

    steps.shouldBe(expected)
  }
}

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

package io.gatling.core.controller.inject.open

import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.duration._

import io.gatling.BaseSpec
import io.gatling.commons.util.Clock
import io.gatling.core.scenario.Scenario

import org.scalacheck.Gen

object OpenInjectionProfileSpec {

  private class FakeClock extends Clock {

    var value: Long = System.currentTimeMillis()

    override def nowMillis: Long = {
      val v = value
      value += 1000
      v
    }
  }

  private def drain(profile: OpenInjectionProfile): Int = {

    var count = 0

    val workload = new OpenWorkload(
      scenario = new Scenario("foo", null, identity, _ => (), null, null, Nil),
      stream = UserStream(profile.steps),
      userIdGen = new AtomicLong,
      startTime = System.currentTimeMillis(),
      duration = profile.steps.foldLeft(Duration.Zero)((acc, step) => acc.plus(step.duration)),
      isEmpty = profile.steps.forall(_.users == 0),
      eventLoopGroup = null,
      statsEngine = null,
      clock = new FakeClock
    ) {

      override protected def injectUser(delay: FiniteDuration): Unit =
        count += 1
    }

    while (!workload.isAllUsersScheduled) {
      workload.injectBatch(1.seconds)
    }

    count
  }
}

class OpenInjectionProfileSpec extends BaseSpec {

  import OpenInjectionProfileSpec._

  "Inserting a pause between steps" should "produce the right number of users" in {

    val steps = Seq(AtOnceOpenInjection(1), NothingForOpenInjection(2.seconds), AtOnceOpenInjection(1))
    val profile = new OpenInjectionProfile(steps)
    profile.totalUserCount shouldBe Some(2)
  }

  "RampOpenInjection" should "produce the expected number of total users" in {

    val validUsers = Gen.choose(1, 100).suchThat(_ > 0)
    val validDurationSeconds = Gen.choose(1, 200).suchThat(_ > 2)

    forAll((validUsers, "users"), (validDurationSeconds, "durationSeconds")) { (users, durationSeconds) =>
      val steps = Seq(RampOpenInjection(users, durationSeconds.second))
      val profile = new OpenInjectionProfile(steps)
      val actualCount = drain(profile)
      profile.totalUserCount shouldBe Some(actualCount)
    }
  }

  "ConstantRateOpenInjection" should "produce the expected number of total users" in {

    val validRate = Gen.choose(0, 100).suchThat(_ > 0)
    val validDurationSeconds = Gen.choose(1, 200).suchThat(_ > 1)

    forAll((validRate, "rate"), (validDurationSeconds, "durationSeconds")) { (startRate, durationSeconds) =>
      val steps = Seq(ConstantRateOpenInjection(startRate, durationSeconds.second))
      val profile = new OpenInjectionProfile(steps)
      val actualCount = drain(profile)
      profile.totalUserCount shouldBe Some(actualCount)
    }
  }

  "RampRateOpenInjection" should "produce the expected number of total users" in {

    val validStartRate = Gen.choose(0, 100).suchThat(_ > 0)
    val validEndRate = Gen.choose(0, 100).suchThat(_ > 0)
    val validDurationSeconds = Gen.choose(1, 200).suchThat(_ > 2)

    forAll((validStartRate, "startRate"), (validEndRate, "endRate"), (validDurationSeconds, "durationSeconds")) { (startRate, endRate, durationSeconds) =>
      val steps = Seq(RampRateOpenInjection(startRate, endRate, durationSeconds.second))
      val profile = new OpenInjectionProfile(steps)
      val actualCount = drain(profile)
      profile.totalUserCount shouldBe Some(actualCount)
    }
  }

  "AtOnceOpenInjection" should "produce the expected number of total users" in {

    val validUsers = Gen.choose(1, 100).suchThat(_ > 0)

    forAll((validUsers, "users")) { users =>
      val steps = Seq(AtOnceOpenInjection(users))
      val profile = new OpenInjectionProfile(steps)
      val actualCount = drain(profile)
      profile.totalUserCount shouldBe Some(actualCount)
    }
  }

  "HeavisideOpenInjection" should "produce the expected number of total users" in {

    val validUsers = Gen.choose(1, 100).suchThat(_ > 0)
    val validDurationSeconds = Gen.choose(1, 200).suchThat(_ > 2)

    forAll((validUsers, "users"), (validDurationSeconds, "durationSeconds")) { (users, durationSeconds) =>
      val steps = Seq(HeavisideOpenInjection(users, durationSeconds.second))
      val profile = new OpenInjectionProfile(steps)
      val actualCount = drain(profile)
      profile.totalUserCount shouldBe Some(actualCount)
    }
  }

  "composite.injectionSteps" should "produce the expected injection profile with starting users and with ramps" in {
    val steps = IncreasingUsersPerSecCompositeStep(
      usersPerSec = 10,
      nbOfSteps = 5,
      duration = 10.seconds,
      startingUsers = 5,
      rampDuration = 20.seconds
    ).composite.steps

    val expected = List(
      ConstantRateOpenInjection(5, 10.seconds),
      RampRateOpenInjection(5, 15, 20.seconds),
      ConstantRateOpenInjection(15, 10.seconds),
      RampRateOpenInjection(15, 25, 20.seconds),
      ConstantRateOpenInjection(25, 10.seconds),
      RampRateOpenInjection(25, 35, 20.seconds),
      ConstantRateOpenInjection(35, 10.seconds),
      RampRateOpenInjection(35, 45, 20.seconds),
      ConstantRateOpenInjection(45, 10.seconds)
    )

    steps.shouldBe(expected)
  }

  it should "produce the expected injection profile without starting users and without ramps" in {
    val steps = IncreasingUsersPerSecCompositeStep(
      usersPerSec = 10,
      nbOfSteps = 5,
      duration = 10.seconds,
      startingUsers = 0,
      rampDuration = Duration.Zero
    ).composite.steps

    val expected = List(
      ConstantRateOpenInjection(0, 10.seconds),
      ConstantRateOpenInjection(10, 10.seconds),
      ConstantRateOpenInjection(20, 10.seconds),
      ConstantRateOpenInjection(30, 10.seconds),
      ConstantRateOpenInjection(40, 10.seconds)
    )

    steps.shouldBe(expected)
  }

  it should "produce the expected injection profile with starting users and without ramps" in {
    val steps = IncreasingUsersPerSecCompositeStep(
      usersPerSec = 10,
      nbOfSteps = 5,
      duration = 10.seconds,
      startingUsers = 5,
      rampDuration = Duration.Zero
    ).composite.steps

    val expected = List(
      ConstantRateOpenInjection(5, 10.seconds),
      ConstantRateOpenInjection(15, 10.seconds),
      ConstantRateOpenInjection(25, 10.seconds),
      ConstantRateOpenInjection(35, 10.seconds),
      ConstantRateOpenInjection(45, 10.seconds)
    )

    steps.shouldBe(expected)
  }

  it should "produce the expected injection profile without starting users and with ramps" in {
    val steps = IncreasingUsersPerSecCompositeStep(
      usersPerSec = 10,
      nbOfSteps = 5,
      duration = 10.seconds,
      startingUsers = 0,
      rampDuration = 80.seconds
    ).composite.steps

    val expected = List(
      RampRateOpenInjection(0, 10, 80.seconds),
      ConstantRateOpenInjection(10, 10.seconds),
      RampRateOpenInjection(10, 20, 80.seconds),
      ConstantRateOpenInjection(20, 10.seconds),
      RampRateOpenInjection(20, 30, 80.seconds),
      ConstantRateOpenInjection(30, 10.seconds),
      RampRateOpenInjection(30, 40, 80.seconds),
      ConstantRateOpenInjection(40, 10.seconds),
      RampRateOpenInjection(40, 50, 80.seconds),
      ConstantRateOpenInjection(50, 10.seconds)
    )

    steps.shouldBe(expected)
  }

  it should "chain components in correct order" in {
    val composite = CompositeOpenInjectionStep(
      List(
        ConstantRateOpenInjection(2, 1.seconds),
        ConstantRateOpenInjection(4, 1.seconds)
      )
    )
    composite.chain(Iterator.empty).toList shouldBe Seq(
      0.milliseconds,
      500.milliseconds,
      1000.milliseconds,
      1250.milliseconds,
      1500.milliseconds,
      1750.milliseconds
    )
  }
}

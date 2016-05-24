/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.core.controller.inject

import scala.concurrent.duration._

import io.gatling.BaseSpec
import io.gatling.commons.util.Collections._

class InjectionStepSpec extends BaseSpec {

  private def scheduling(steps: InjectionStep*): List[FiniteDuration] =
    steps.reverse.foldLeft[Iterator[FiniteDuration]](Iterator.empty) { (it, step) =>
      step.chain(it)
    }.toList

  "RampInjection" should "return the correct number of users" in {
    RampInjection(5, 1 second).users shouldBe 5
  }

  it should "return the correct injection duration" in {
    RampInjection(5, 1 second).duration shouldBe (1 second)
  }

  it should "schedule with a correct interval" in {

    val ramp = RampInjection(5, 1 second)
    val rampScheduling = scheduling(ramp)
    val interval0 = rampScheduling(1) - rampScheduling.head
    val interval1 = rampScheduling(2) - rampScheduling(1)

    rampScheduling.length shouldBe ramp.users
    interval0 shouldBe interval1
    interval0 shouldBe (200 milliseconds)
  }

  it should "schedule the correct number of users" in {
    val step = RampInjection(3, 8 seconds)
    step.users shouldBe 3
    scheduling(step).size shouldBe 3
  }

  it should "the first and the last users should be correctly scheduled" in {
    val rampScheduling = scheduling(RampInjection(5, 1 second))
    val first = rampScheduling.head
    val last = rampScheduling.last

    first shouldBe Duration.Zero
    last shouldBe <(1 second)
    rampScheduling shouldBe sorted
  }

  "ConstantRateInjection" should "return the correct number of users" in {
    ConstantRateInjection(1.0, 5 seconds).users shouldBe 5
    ConstantRateInjection(0.4978, 100 seconds).users shouldBe 50
  }

  "NothingForInjection" should "return the correct number of users" in {
    NothingForInjection(1 second).users shouldBe 0
  }

  it should "return the correct injection duration" in {
    NothingForInjection(1 second).duration shouldBe (1 second)
  }

  it should "return the correct injection scheduling" in {
    NothingForInjection(1 second).chain(Iterator.empty) shouldBe empty
  }

  "AtOnceInjection" should "return the correct number of users" in {
    AtOnceInjection(4).users shouldBe 4
  }

  it should "return the correct injection duration" in {
    scheduling(AtOnceInjection(4)).max shouldBe Duration.Zero
  }

  it should "return the correct injection scheduling" in {
    val peak = AtOnceInjection(4)
    val atOnceScheduling = scheduling(peak)
    val uniqueScheduling = atOnceScheduling.toSet
    uniqueScheduling should contain(Duration.Zero)
    atOnceScheduling should have length peak.users
  }

  "RampRateInjection" should "return the correct injection duration" in {
    RampRateInjection(2, 4, 10 seconds).duration shouldBe (10 seconds)
  }

  it should "return the correct number of users" in {
    RampRateInjection(2, 4, 10 seconds).users shouldBe 30
  }

  it should "provides an injection scheduling with the correct number of elements" in {
    val rampRate = RampRateInjection(2, 4, 10 seconds)
    val rampRateScheduling = scheduling(rampRate)
    rampRateScheduling.length shouldBe rampRate.users
  }

  it should "provides an injection scheduling with the correct values" in {
    val rampRateScheduling = scheduling(RampRateInjection(2, 4, 10 seconds))
    rampRateScheduling.head shouldBe Duration.Zero
    rampRateScheduling(1) shouldBe (500 milliseconds)
  }

  it should "return the correct injection duration when the acceleration is null" in {
    RampRateInjection(1.0, 1.0, 10 seconds).duration shouldBe (10 seconds)
  }

  it should "return the correct number of users when the acceleration is null" in {
    RampRateInjection(1.0, 1.0, 10 seconds).users shouldBe 10
  }

  it should "return a scheduling of constant step when the acceleration is null" in {

    val constantRampScheduling = scheduling(RampRateInjection(1.0, 1.0, 10 seconds))

    val steps = constantRampScheduling.zip(constantRampScheduling.drop(1)).map {
      case (i1, i2) => i2 - i1
    }.toSet[FiniteDuration]

    constantRampScheduling shouldBe sorted
    steps.size shouldBe 1
    constantRampScheduling.last shouldBe <(10 seconds)
  }

  "SplitInjection" should "provide an appropriate injection scheduling and ignore extra users" in {
    val scheduling = SplitInjection(6, RampInjection(2, 2 seconds), NothingForInjection(5 seconds)).chain(Iterator.empty).toList
    scheduling shouldBe List(
      Duration.Zero, 1 second, // 1st ramp
      7 seconds, 8 seconds, // 2nd ramp after a pause
      14 seconds, 15 seconds
    ) // 3rd ramp after a pause
  }

  it should "should schedule the first and last user through the 'into' injection step" in {
    val scheduling = SplitInjection(5, RampInjection(2, 2 seconds), AtOnceInjection(1)).chain(AtOnceInjection(1).chain(Iterator.empty)).toList
    scheduling shouldBe List(
      Duration.Zero, 1 second, // 1st ramp
      2 seconds, // at once in between
      2 seconds, 3 seconds, // 2nd ramp until reaching 5 users
      4 seconds
    ) // at once from the chained injection
  }

  val heavisideScheduling = HeavisideInjection(100, 5 seconds).chain(Iterator.empty).toList
  "HeavisideInjection" should "provide an appropriate number of users" in {
    heavisideScheduling.length shouldBe 100
  }

  it should "provide correct values" in {
    heavisideScheduling(1) shouldBe (291 milliseconds)
    heavisideScheduling shouldBe sorted
    heavisideScheduling.last shouldBe <(5 seconds)
  }

  it should "have most of the scheduling values close to half of the duration" in {
    val l = heavisideScheduling.count((t) => (t > (1.5 seconds)) && (t < (3.5 seconds)))
    l shouldBe 67 // two thirds
  }

  "Injection chaining" should "provide a monotonically increasing series of durations" in {
    val scheduling = RampInjection(3, 2 seconds).chain(RampInjection(3, 2 seconds).chain(Iterator.empty)).toVector
    scheduling shouldBe sorted
  }

  "Poisson injection" should "inject constant users at approximately the right rate" in {
    // Inject 1000 users per second for 60 seconds
    val inject = PoissonInjection(60 seconds, 1000.0, 1000.0, seed = 0L) // Seed with 0, to ensure tests are deterministic
    val scheduling = inject.chain(Iterator(0.seconds)).toVector // Chain to an injector with a zero timer
    scheduling.size shouldBe (inject.users + 1)
    scheduling.size shouldBe 60001 +- 200 // 60000 for the users injected by PoissonInjection, plus the 0 second one
    scheduling.last shouldBe (60 seconds)
    scheduling(scheduling.size - 2).toMillis shouldBe 60000L +- 5L
    scheduling.head.toMillis shouldBe 0L +- 5L
    scheduling(30000).toMillis shouldBe 30000L +- 1000L // Half-way through we should have injected half of the users
  }

  it should "inject ramped users at approximately the right rate" in {
    // ramp from 0 to 1000 users per second over 60 seconds
    val inject = PoissonInjection(60.seconds, 0.0, 1000.0, seed = 0L) // Seed with 0, to ensure tests are deterministic
    val scheduling = inject.chain(Iterator(0.seconds)).toVector // Chain to an injector with a zero timer
    scheduling.size shouldBe (inject.users + 1)
    scheduling.size shouldBe 30001 +- 500 // 30000 for the users injected by PoissonInjection, plus the 0 second one
    scheduling.last shouldBe (60 seconds)
    scheduling(scheduling.size - 2).toMillis shouldBe 60000L +- 5L
    scheduling.head.toMillis shouldBe 0L +- 200L
    scheduling(7500).toMillis shouldBe 30000L +- 1000L // Half-way through ramp-up we should have run a quarter of users
  }

  "Chain steps" should "inject the expected number of users" in {
    val steps = Vector(
      RampInjection(50, 9 minutes),
      NothingForInjection(1 minute),
      RampInjection(50, 1 minute),
      NothingForInjection(9 minutes),
      RampInjection(50, 1 minute),
      NothingForInjection(9 minutes),
      RampInjection(50, 1 minute),
      NothingForInjection(9 minutes),
      RampInjection(50, 1 minute),
      NothingForInjection(9 minutes)
    )

    scheduling(steps: _*).size shouldBe steps.sumBy(_.users)
  }
}

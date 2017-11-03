/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
import io.gatling.core.controller.inject.Injector._
import io.gatling.commons.util.PushbackIterator
import io.gatling.commons.util.ClockSingleton._
import io.gatling.core.scenario.Scenario

class UserStreamSpec extends BaseSpec {

  "UserStream" should "stream users properly over a long period" in {

    val scenario = Scenario("scenario", null, null, null, null, null)
    val expectedTotalUsers = 9000
    val expectedDuration = 9 hours
    val ramp = RampInjection(expectedTotalUsers, expectedDuration)
    val injectionProfile = InjectionProfile(List(ramp))

    val startTime = nowMillis

    val userStream = UserStream(scenario, new PushbackIterator(injectionProfile.allUsers))

    var injectedUsers = 0
    var count = 0
    var cont = true

    var lastBatchTimeSinceStart = 0
    var lastBatchMaxOffset = Duration.Zero

    while (cont) {
      // batches are scheduled every 1 second
      lastBatchTimeSinceStart = count * (TickPeriod.toMillis.toInt + 5) // 5 ms scheduler drift on each iteration

      val injection = userStream.withStream(InitialBatchWindow, lastBatchTimeSinceStart + startTime, startTime) {
        case (_, duration) =>
          injectedUsers += 1
          // calls are sorted
          lastBatchMaxOffset = duration
      }

      count += 1
      cont = injection.continue
    }

    injectedUsers shouldBe expectedTotalUsers

    val lastSchedulingOffset = lastBatchMaxOffset + lastBatchTimeSinceStart.millis
    lastSchedulingOffset.toMillis shouldBe expectedDuration.toMillis +- (4 seconds).toMillis
  }

  it should "continue injecting after first batch" in {

    val scenario = Scenario("scenario", null, null, null, null, null)
    val ramp = RampInjection(144000000 / 30, 18000 seconds)
    val injectionProfile = InjectionProfile(List(ramp))

    val startTime = nowMillis

    for (timeSinceStart <- -1001 until 2000) {
      val userStream = UserStream(scenario, new PushbackIterator(injectionProfile.allUsers))

      val injection = userStream.withStream(InitialBatchWindow, timeSinceStart + startTime, startTime) {
        case _ => // nothing
      }

      injection.continue shouldBe true
    }
  }

  // [fl]
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  // [fl]
}

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
import io.gatling.commons.util.PushbackIterator
import io.gatling.commons.util.TimeHelper._
import io.gatling.core.scenario.Scenario
import io.gatling.core.util.Shard

import com.softwaremill.quicklens._

class UserStreamSpec extends BaseSpec {

  "UserStream" should "continue injecting after first batch" in {

    val scenario = Scenario("scenario", null, null, null, null, null)
    val ramp = RampInjection(144000000 / 30, 18000 seconds)
    val initialBatchWindow = 2 seconds
    val nodeCount = 30

    val startTime = nowMillis

    for {
      nodeId <- 0 until nodeCount
      shardCount = Shard.shard(_: Int, nodeId, nodeCount).length
      shardedRamp = ramp.modify(_.users).using(shardCount)
      injectionProfile = InjectionProfile(List(shardedRamp))
      timeSinceStart <- -1001 until 2000
    } {
      val userStream = UserStream(scenario, new PushbackIterator(injectionProfile.allUsers))

      val injection = userStream.withStream(initialBatchWindow, timeSinceStart + startTime, startTime) {
        case (scn, duration) => // nothing
      }

      injection.continue shouldBe true
    }
  }
}

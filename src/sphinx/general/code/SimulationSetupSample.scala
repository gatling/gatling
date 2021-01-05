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

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class SimulationSetupSample extends Simulation {

  private val httpProtocol = http
  private val scn = scenario("scenario")

  //#open-injection
  setUp(
    scn.inject(
      nothingFor(4.seconds), // 1
      atOnceUsers(10), // 2
      rampUsers(10).during(5.seconds), // 3
      constantUsersPerSec(20).during(15.seconds), // 4
      constantUsersPerSec(20).during(15.seconds).randomized, // 5
      rampUsersPerSec(10).to(20).during(10.minutes), // 6
      rampUsersPerSec(10).to(20).during(10.minutes).randomized, // 7
      heavisideUsers(1000).during(20.seconds) // 8
    ).protocols(httpProtocol)
  )
  //#open-injection

  //#closed-injection
  setUp(
    scn.inject(
      constantConcurrentUsers(10).during(10.seconds), // 1
      rampConcurrentUsers(10).to(20).during(10.seconds) // 2
    )
  )
  //#closed-injection

  //#throttling
  setUp(scn.inject(constantUsersPerSec(100).during(30.minutes))).throttle(
    reachRps(100).in(10.seconds),
    holdFor(1.minute),
    jumpToRps(50),
    holdFor(2.hours)
  )
  //#throttling

  //#max-duration
  setUp(scn.inject(rampUsers(1000).during(20.minutes))).maxDuration(10.minutes)
  //#max-duration

  //#incrementConcurrentUsers
  // generate a closed workload injection profile
  // with levels of 10, 15, 20, 25 and 30 concurrent users
  // each level lasting 10 seconds
  // separated by linear ramps lasting 10 seconds
  setUp(
    scn.inject(
      incrementConcurrentUsers(5) // Int
        .times(5)
        .eachLevelLasting(10.seconds)
        .separatedByRampsLasting(10.seconds)
        .startingFrom(10) // Int
    )
  )
  //#incrementConcurrentUsers

  //#incrementUsersPerSec
  // generate an open workload injection profile
  // with levels of 10, 15, 20, 25 and 30 arriving users per second
  // each level lasting 10 seconds
  // separated by linear ramps lasting 10 seconds
  setUp(
    scn.inject(
      incrementUsersPerSec(5) // Double
        .times(5)
        .eachLevelLasting(10.seconds)
        .separatedByRampsLasting(10.seconds)
        .startingFrom(10) // Double
    )
  )
  //#incrementUsersPerSec

  private val scenario1 = scenario("scenario1")
  private val scenario2 = scenario("scenario2")
  private val injectionProfile1 = atOnceUsers(1)
  private val injectionProfile2 = atOnceUsers(1)

  //#multiple
  setUp(
    scenario1.inject(injectionProfile1),
    scenario2.inject(injectionProfile2)
  )
  //#multiple

  private val parent = scenario("parent")
  private val child1 = scenario("child1")
  private val child2 = scenario("child2")
  private val grandChild = scenario("grandChild")
  private val injectionProfile = constantConcurrentUsers(5).during(5)

  //#andThen
  setUp(
    parent
      .inject(injectionProfile)
      // child1 and child2 will start at the same time when last parent user will terminate
      .andThen(
        child1
          .inject(injectionProfile)
          // grandChild will start when last child1 user will terminate
          .andThen(grandChild.inject(injectionProfile)),
        child2.inject(injectionProfile)
      )
  )
  //#andThen

  //#noShard
  setUp(
    // parent load won't be sharded
    parent
      .inject(atOnceUsers(1))
      .noShard
      .andThen(
        // child load will be sharded
        child1.inject(injectionProfile)
      )
  )
  //#noShard
}

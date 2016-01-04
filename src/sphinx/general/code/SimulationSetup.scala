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
import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class SimulationSetup extends Simulation {

  val httpConf = http
  val scn = scenario("scenario")

  //#injection
  setUp(
    scn.inject(
      nothingFor(4 seconds), // 1
      atOnceUsers(10), // 2
      rampUsers(10) over(5 seconds), // 3
      constantUsersPerSec(20) during(15 seconds), // 4
      constantUsersPerSec(20) during(15 seconds) randomized, // 5
      rampUsersPerSec(10) to 20 during(10 minutes), // 6
      rampUsersPerSec(10) to 20 during(10 minutes) randomized, // 7
      splitUsers(1000) into(rampUsers(10) over(10 seconds)) separatedBy(10 seconds), // 8
      splitUsers(1000) into(rampUsers(10) over(10 seconds)) separatedBy atOnceUsers(30), // 9
      heavisideUsers(1000) over(20 seconds) // 10
    ).protocols(httpConf)
  )
  //#injection

  //#throttling
  setUp(scn.inject(constantUsersPerSec(100) during(30 minutes))).throttle(
    reachRps(100) in (10 seconds),
    holdFor(1 minute),
    jumpToRps(50),
    holdFor(2 hours)
  )
  //#throttling

  //#max-duration
  setUp(scn.inject(rampUsers(1000) over(20 minutes))).maxDuration(10 minutes)
  //#max-duration
}

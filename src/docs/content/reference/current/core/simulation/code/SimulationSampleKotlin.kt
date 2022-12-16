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

//#imports
// required for Gatling core structure DSL
import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*

// required for Gatling HTTP DSL
import io.gatling.javaapi.http.*
import io.gatling.javaapi.http.HttpDsl.*

// can be omitted if you don't use jdbcFeeder
import io.gatling.javaapi.jdbc.*
import io.gatling.javaapi.jdbc.JdbcDsl.*

// used for specifying durations with a unit, eg Duration.ofMinutes(5)
import java.time.Duration
//#imports


class SimulationSampleKotlin : Simulation() {

  val httpProtocol = http
  val httpProtocol1 = http
  val httpProtocol2 = http

  init {
//#setUp
val scn = scenario("scn") // etc...

setUp(
  scn.injectOpen(atOnceUsers(1))
)
//#setUp

//#setUp-multiple
val scn1 = scenario("scn1") // etc...
val scn2 = scenario("scn2") // etc...

setUp(
  scn1.injectOpen(atOnceUsers(1)),
  scn2.injectOpen(atOnceUsers(1))
)
//#setUp-multiple

//#protocols
// HttpProtocol configured globally
setUp(
  scn1.injectOpen(atOnceUsers(1)),
  scn2.injectOpen(atOnceUsers(1))
).protocols(httpProtocol)

// different HttpProtocols configured on each population
setUp(
  scn1.injectOpen(atOnceUsers(1))
    .protocols(httpProtocol1),
  scn2.injectOpen(atOnceUsers(1))
    .protocols(httpProtocol2)
)
//#protocols

//#assertions
setUp(scn.injectOpen(atOnceUsers(1)))
  .assertions(global().failedRequests().count().shouldBe(0L))
//#assertions

//#pauses
// pause configuration configured globally
setUp(scn.injectOpen(atOnceUsers(1)))
  // disable the pauses for the simulation
  .disablePauses()
  // the duration of each pause is what's specified
  // in the `pause(duration)` element.
  .constantPauses()
  // make pauses follow a uniform distribution
  // where the mean is the value specified in the `pause(duration)` element.
  .uniformPauses(0.5)
  .uniformPauses(Duration.ofSeconds(2))
  // make pauses follow a normal distribution
  // where the mean is the value specified in the `pause(duration)` element.
  // and the standard deviation is the duration configured here.
  .normalPausesWithStdDevDuration(Duration.ofSeconds(2))
  // make pauses follow a normal distribution
  // where the mean is the value specified in the `pause(duration)` element.
  // and the standard deviation is a percentage of the mean.
  .normalPausesWithPercentageDuration(20.0)
  // make pauses follow an exponential distribution
  // where the mean is the value specified in the `pause(duration)` element.
  .exponentialPauses()
  // the pause duration is computed by the provided function (in milliseconds).
  // In this case the filled duration is bypassed.
  .customPauses { session -> 5L }

// different pause configurations configured on each population
setUp(
  scn1.injectOpen(atOnceUsers(1))
    .disablePauses(),
  scn2.injectOpen(atOnceUsers(1))
    .exponentialPauses()
)
//#pauses

//#throttling
// throttling profile configured globally
setUp(scn.injectOpen(constantUsersPerSec(100.0).during(Duration.ofMinutes(30))))
  .throttle(
    reachRps(100).during(10),
    holdFor(Duration.ofMinutes(1)),
    jumpToRps(50),
    holdFor(Duration.ofHours(2))
  )

// different throttling profiles configured globally
setUp(
  scn1.injectOpen(atOnceUsers(1))
    .throttle(reachRps(100).during(10)),
  scn2.injectOpen(atOnceUsers(1))
    .throttle(reachRps(20).during(10))
)
//#throttling

//#max-duration
setUp(scn.injectOpen(rampUsers(1000).during(Duration.ofMinutes(20))))
  .maxDuration(Duration.ofMinutes(10))
//#max-duration
}

//#hooks
override fun before() {
  println("Simulation is about to start!")
}

override fun after() {
  println("Simulation is finished!")
}
//#hooks
}

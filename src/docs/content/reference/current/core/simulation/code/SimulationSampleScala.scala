/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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
import io.gatling.core.Predef._

// required for Gatling HTTP DSL
import io.gatling.http.Predef._

// can be omitted if you don't use jdbcFeeder
import io.gatling.jdbc.Predef._

// used for specifying durations with a unit, eg "5 minutes"
import scala.concurrent.duration._
//#imports

class SimulationSampleScala extends Simulation {
  val httpProtocol, httpProtocol1, httpProtocol2 = http

//#setUp
val scn = scenario("scn") // etc...

setUp(
  scn.inject(atOnceUsers(1))
)
//#setUp

//#setUp-multiple
val scn1 = scenario("scn1") // etc...
val scn2 = scenario("scn2") // etc...

setUp(
  scn1.inject(atOnceUsers(1)),
  scn2.inject(atOnceUsers(1))
);
//#setUp-multiple

//#protocols
// HttpProtocol configured globally
setUp(
  scn1.inject(atOnceUsers(1)),
  scn2.inject(atOnceUsers(1))
).protocols(httpProtocol)

// different HttpProtocols configured on each population
setUp(
  scn1.inject(atOnceUsers(1))
    .protocols(httpProtocol1),
  scn2.inject(atOnceUsers(1))
    .protocols(httpProtocol2)
)
//#protocols

//#assertions
setUp(scn.inject(atOnceUsers(1)))
  .assertions(global.failedRequests.count.is(0))
//#assertions

//#pauses
// pause configuration configured globally
setUp(scn.inject(atOnceUsers(1)))
  // disable the pauses for the simulation
  .disablePauses
  // the duration of each pause is what's specified
  // in the `pause(duration)` element.
  .constantPauses
  // make pauses follow a uniform distribution
  // where the mean is the value specified in the `pause(duration)` element.
  .uniformPauses(0.5)
  .uniformPauses(2.seconds)
  // make pauses follow a normal distribution
  // where the mean is the value specified in the `pause(duration)` element.
  // and the standard deviation is the duration configured here.
  .normalPausesWithStdDevDuration(2.seconds)
  // make pauses follow a normal distribution
  // where the mean is the value specified in the `pause(duration)` element.
  // and the standard deviation is a percentage of the mean.
  .normalPausesWithPercentageDuration(20.0)
  // make pauses follow an exponential distribution
  // where the mean is the value specified in the `pause(duration)` element.
  .exponentialPauses
  // the pause duration is computed by the provided function (in milliseconds).
  // In this case the filled duration is bypassed.
  .customPauses(session => 5L)

// different pause configurations configured on each population
setUp(
  scn1.inject(atOnceUsers(1))
    .disablePauses,
  scn2.inject(atOnceUsers(1))
    .exponentialPauses
)
//#pauses

//#throttling
// throttling profile configured globally
setUp(scn.inject(constantUsersPerSec(100).during(30.minutes)))
  .throttle(
    reachRps(100).in(10),
    holdFor(1.minute),
    jumpToRps(50),
    holdFor(2.hours)
  )

// different throttling profiles configured globally
setUp(
  scn1.inject(atOnceUsers(1))
    .throttle(reachRps(100).in(10)),
  scn2.inject(atOnceUsers(1))
    .throttle(reachRps(20).in(10))
)
//#throttling

//#max-duration
setUp(scn.inject(rampUsers(1000).during(20.minutes)))
  .maxDuration(10.minutes)
//#max-duration

//#hooks
before {
  println("Simulation is about to start!")
}

after {
  println("Simulation is finished!")
}
//#hooks
}

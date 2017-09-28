/**
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
package io.gatling.core.compile

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.core.protocol.Protocol
import io.gatling.core.structure.ChainBuilder

class CoreCompileTest extends Simulation {

  val iterations = 10
  val pause1 = 1
  val pause2 = 2
  val pause3 = 3
  val pause4 = Integer.getInteger("testProperty")

  val pause5 = pause4 milliseconds
  val pause6 = pause4 seconds
  val pause7 = pause4 nanoseconds

  val baseUrl = "http://localhost:3000"

  val noop: ChainBuilder = ???
  val protocol: Protocol = ???

  val usersInformation = tsv("user_information.tsv")

  val loginChain = exec(noop).pause(1)

  val testData = tsv("test-data.tsv")

  feed(csv("foo.csv").batched)
  feed(csv("foo.csv").batched.random)
  feed(csv("foo.csv").batched(500))
  feed(csv("foo.csv").batched(500).random)

  val richTestData = testData.convert { case ("keyOfAMultivaluedColumn", value) => value.split(",") }

  val testData3 = Array(Map("foo" -> "bar")).circular

  val lambdaUser = scenario("Standard User")
    // First request outside iteration
    .repeat(2) {
      feed(richTestData)
        .exec(noop)
    }
    .repeat(2, "counterName") {
      feed(testData.circular)
        .exec(noop)
    }
    .during(10 seconds) {
      feed(testData)
        .exec(noop)
    }
    .forever {
      feed(testData)
        .exec(noop)
    }
    .group("group") {
      exec(noop)
    }
    .exec(noop)
    .uniformRandomSwitch(exec(noop), exec(noop))
    .randomSwitch(
      40d -> exec(noop),
      50d -> exec(noop)
    )
    .randomSwitch(40d -> exec(noop))
    .randomSwitchOrElse(
      50d -> exec(noop)
    )(exec(noop))
    .pause(pause2)
    // Loop
    .doWhile("${condition}") {
      exec(noop)
    }
    .doWhile("${condition}", "counterName") {
      exec(noop)
    }
    .asLongAsDuring("${condition}", 10 seconds) {
      exec(noop)
    }
    .asLongAsDuring("${condition}", 10 seconds, "counterName") {
      exec(noop)
    }
    .doWhileDuring("${condition}", 10 seconds) {
      exec(noop)
    }
    .doWhileDuring("${condition}", 10 seconds, "counterName") {
      exec(noop)
    }
    .repeat(iterations, "counter") {
      // What will be repeated ?
      // First request to be repeated
      exec { session =>
        println("iterate: " + session("counter"))
        session
      }
        .exec(noop)
        .during(12000 milliseconds, "foo") {
          exec(noop)
            .pause(2, constantPauses)
            .repeat(2, "tutu") {
              exec { session =>
                println("--nested loop: " + session("tutu"))
                session
              }
            }
            .exec { session =>
              println("-loopDuring: " + session("foo"))
              session
            }
            .exec(noop)
            .pause(2)
        }
        .pause(pause2)
        .during(12000 milliseconds, "duringCount") {
          exec(noop)
            .pause(2)
            .exec { session =>
              println("-iterate1: " + session("counter") + ", doFor: " + session("duringCount"))
              session
            }
            .repeat(2, "count") {
              exec { session =>
                println("--iterate1: " + session("counter") + ", doFor: " + session("duringCount") + ", iterate2: " + session("count"))
                session
              }
            }
            .exec(noop)
            .pause(2)
        }
        .exec(session => session.set("test2", "bbbb"))
        .doIfEqualsOrElse("test2", "aaaa") {
          exec(noop)
        } {
          exec(noop)
        }.pause(pause2)
        // switch
        .randomSwitch(
          40d -> exec(noop),
          55d -> exec(noop) // last 5% bypass
        )
    }
    // Head request
    .exec(noop)
    .rendezVous(100)
    .exec(noop)
    .pace(5)
    .pace(5 seconds)
    .pace("${foo}")
    .pace(5, 10)
    .pace("${foo}", "${bar}")
    .doSwitch("${foo}")(
      "a" -> exec(noop),
      "b" -> exec(noop)
    )
    .doSwitchOrElse("${foo}")(
      "a" -> exec(noop),
      "b" -> exec(noop) //
    )(exec(noop))
    .exec(noop)
    .exec(session => session.set("tryMax", 3))
    .tryMax("${tryMax}") {
      exec(noop)
    }

  val inject1 = nothingFor(10 milliseconds)
  val inject2 = rampUsers(10).over(10 minutes)
  val inject3 = constantUsersPerSec(10).during(1 minute)
  val inject4 = atOnceUsers(100)
  val inject5 = rampUsersPerSec(10) to 20 during (10 minutes)
  val inject6 = splitUsers(1000).into(rampUsers(10) over (10 seconds)).separatedBy(10 seconds)
  val inject7 = splitUsers(1000).into(rampUsers(10) over (10 seconds)).separatedBy(atOnceUsers(30))
  val inject8 = heavisideUsers(1000) over (20 seconds)

  val injectionSeq = Vector(1, 2, 4, 8).map(x => rampUsers(x * 100) over (5 seconds))
  setUp(
    lambdaUser.inject(inject1),
    lambdaUser.inject(injectionSeq: _*),
    lambdaUser.inject(inject1, inject2).throttle(jumpToRps(20), reachRps(40) in (10 seconds), holdFor(30 seconds))
  )
    .protocols(protocol)
    .pauses(uniformPausesPlusOrMinusPercentage(1))
    .disablePauses
    .constantPauses
    .exponentialPauses
    .uniformPauses(1.5)
    .uniformPauses(1337 seconds)
    .assertions(
      global.responseTime.mean.lte(50),
      global.responseTime.max.between(50, 500),
      global.successfulRequests.count.gte(1500),
      global.responseTime.percentile1.lt(100),
      global.responseTime.percentile(99.999).lt(100),
      global.allRequests.percent.is(100),
      forAll.failedRequests.percent.is(0),
      forAll.responseTime.max.is(100),
      details("Users" / "Search" / "Index page").responseTime.mean.gt(0),
      details("Admins" / "Create").failedRequests.percent.lt(90),
      details("request_9").requestsPerSec.gte(10)
    )
    .throttle(jumpToRps(20), reachRps(40) in (10 seconds), holdFor(30 seconds))
    // Applies on the setup
    .constantPauses
    .disablePauses
    .exponentialPauses
    .uniformPauses(1.5)
    .uniformPauses(1337 seconds)

  //[fl]
  //
  //[fl]
}

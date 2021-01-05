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

package io.gatling.core.compile

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.core.protocol.Protocol
import io.gatling.core.structure.ChainBuilder

class CoreCompileTest extends Simulation {

  private val iterations = 10
  private val pause1 = 2
  private val pause2 = Integer.getInteger("testProperty")
  private val pause3 = pause2.milliseconds
  private val baseUrl = "http://localhost:3000"

  private val noop: ChainBuilder = ???
  private val protocol: Protocol = ???

  private val usersInformation = tsv("user_information.tsv")

  private val loginChain = exec(noop).pause(1)

  private val testData = tsv("test-data.tsv")

  feed(csv("foo.csv.zip").unzip)
  feed(csv("foo.csv").eager)
  feed(csv("foo.csv").batch)
  feed(csv("foo.csv").unzip.batch.random)
  feed(csv("foo.csv").batch(500))
  feed(csv("foo.csv").batch(500).random)
  feed(csv("foo.csv.zip"), 5)
  feed(jsonFile("foo.json"))
  feed(jsonFile("foo.json").unzip)
  feed(jsonFile("foo.json").unzip.random)
  feed(jsonUrl("http://foo.com"))
  feed(jsonUrl("http://foo.com").random)

  private val records: Seq[Map[String, Any]] = csv("foo.csv").readRecords

  private val richTestData = testData.convert { case ("keyOfAMultivaluedColumn", value) => value.split(",") }

  private val testData3 = Array(Map("foo" -> "bar")).circular

  private val chainedScenarios = exec(scenario("foo")).exec(scenario("bar"))

  private val lambdaUser = scenario("Standard User")
    // First request outside iteration
    .repeat(2) {
      feed(richTestData)
        .exec(noop)
    }
    .repeat(2, "counterName") {
      feed(testData.circular)
        .exec(noop)
    }
    .during(10.seconds) {
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
    .asLongAsDuring("${condition}", 10.seconds) {
      exec(noop)
    }
    .asLongAsDuring("${condition}", 10.seconds, "counterName") {
      exec(noop)
    }
    .doWhileDuring("${condition}", 10.seconds) {
      exec(noop)
    }
    .doWhileDuring("${condition}", 10.seconds, "counterName") {
      exec(noop)
    }
    .repeat(iterations, "counter") {
      // What will be repeated ?
      // First request to be repeated
      exec { session =>
        println(s"iterate: ${session("counter").as[Int]}")
        session
      }.exec(noop)
        .during(12000.milliseconds, "foo") {
          exec(noop)
            .pause(2, constantPauses)
            .repeat(2, "tutu") {
              exec { session =>
                println(s"--nested loop: ${session("tutu").as[Int]}")
                session
              }
            }
            .exec { session =>
              println(s"-loopDuring: ${session("foo").as[Int]}")
              session
            }
            .exec(noop)
            .pause(2)
        }
        .pause(pause2)
        .during(12000.milliseconds, "duringCount") {
          exec(noop)
            .pause(2)
            .exec { session =>
              println(s"-iterate1: ${session("counter").as[Int]}, doFor: ${session("duringCount").as[Int]}")
              session
            }
            .repeat(2, "count") {
              exec { session =>
                println(s"--iterate1: ${session("counter").as[Int]}, doFor: ${session("duringCount").as[Int]}, iterate2: ${session("count").as[Int]}")
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
        }
        .pause(pause2)
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
    .pace(5.seconds)
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
    // tryMax and exit
    .tryMax("${tryMax}") {
      exec(noop)
    }
    .exitHereIfFailed
    .exitBlockOnFail {
      exec(noop)
    }

  private val inject1 = nothingFor(10.milliseconds)
  private val inject2 = rampUsers(10).during(10.minutes)
  private val inject3 = constantUsersPerSec(10).during(1.minute)
  private val inject4 = atOnceUsers(100)
  private val inject5 = rampUsersPerSec(10).to(20).during(10.minutes)
  private val inject8 = heavisideUsers(1000).during(20.seconds)

  private val injectionSeq = Vector(1, 2, 4, 8).map(x => rampUsers(x * 100).during(5.seconds))

  private val closedInject1 = constantConcurrentUsers(100).during(10.seconds)
  private val closedInject2 = rampConcurrentUsers(100).to(200).during(10.seconds)

  private val openSeq = Seq(inject1, inject2, inject3)
  private val closedSeq = Seq(closedInject1, closedInject2)

  private val openMeta = incrementUsersPerSec(5).times(5).eachLevelLasting(10).separatedByRampsLasting(10).startingFrom(10)
  private val closedMeta = incrementConcurrentUsers(5).times(5).eachLevelLasting(10).separatedByRampsLasting(10).startingFrom(10)

  setUp(
    lambdaUser.inject(inject1),
    lambdaUser.inject(injectionSeq),
    lambdaUser.inject(inject1, inject2).throttle(jumpToRps(20), reachRps(40) in (10.seconds), holdFor(30.seconds)),
    lambdaUser.inject(closedInject1, closedInject2),
    lambdaUser.inject(openSeq),
    lambdaUser.inject(closedSeq),
    lambdaUser.inject(openMeta, inject1),
    lambdaUser.inject(closedMeta, closedInject1),
    lambdaUser.inject(inject1).noShard.andThen(lambdaUser.inject(inject2))
  ).protocols(protocol)
    .pauses(uniformPausesPlusOrMinusPercentage(1))
    .disablePauses
    .constantPauses
    .exponentialPauses
    .uniformPauses(1.5)
    .uniformPauses(1337.seconds)
    .assertions(
      global.responseTime.mean.lte(50),
      global.responseTime.max.between(50, 500),
      global.responseTime.max.around(50, 5),
      global.successfulRequests.count.gte(1500),
      global.responseTime.percentile1.lt(100),
      global.responseTime.percentile(99.999).lt(100),
      global.allRequests.percent.is(100),
      forAll.failedRequests.percent.is(0),
      forAll.responseTime.max.is(100),
      global.responseTime.percentile(99).deviatesAround(100, 5),
      details("Users" / "Search" / "Index page").responseTime.mean.gt(0),
      details("Admins" / "Create").failedRequests.percent.lt(90),
      details("request_9").requestsPerSec.gte(10)
    )
    .throttle(jumpToRps(20), reachRps(40) in (10.seconds), holdFor(30.seconds))
    .throttle(Seq(jumpToRps(20), reachRps(40) in (10.seconds), holdFor(30.seconds)))
    // Applies on the setup
    .constantPauses
    .disablePauses
    .exponentialPauses
    .uniformPauses(1.5)
    .uniformPauses(1337.seconds)

  feed(csv("foo.csv").shard)
}

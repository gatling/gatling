/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import java.io.ByteArrayInputStream

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.core.feeder.Record

class CoreCompileTest extends Simulation {

  // execs
  private val chain1 = exec(session => session)
  private val chain2 = exec(chain1, chain1).exec(chain1)
  // pauses
  private val pause1 = pause(1)
  // loops
  private val loop1 = repeat(1) {
    chain1
  }
  // groups
  private val group1 = group("group") {
    chain1
  }
  private val group2 = group(session => "group") {
    chain1
  }

  // bodies
  private val stringBody1 = StringBody("static #{dynamic} static")
  private val stringBody2 = StringBody(session => "body")
  private val rawFileBody1 = RawFileBody("path")
  private val rawFileBody2 = RawFileBody(session => "path")
  private val elFileBody1 = ElFileBody("path")
  private val elFileBody2 = ElFileBody(session => "path")
  private val pebbleStringBody = PebbleStringBody("template string")
  private val pebbleFileBody1 = PebbleFileBody("path")
  private val pebbleFileBody2 = PebbleFileBody(session => "path")
  private val byteArrayBody1 = ByteArrayBody(Array[Byte](1))
  private val byteArrayBody2 = ByteArrayBody(session => Array[Byte](1))
  private val inputStreamBody = InputStreamBody(session => new ByteArrayInputStream(Array[Byte](1)))

  // scenario
  private val scn =
    scenario("scenario")
      // execs
      .exec(session => session)
      .exec(chain1, chain2)
      .exec(List(chain1))
      // groups
      .group("group") {
        chain1
      }
      .group(session => "group") {
        chain1
      }

  private val feeds = scenario("feeds")
    .feed(csv("foo"))
    .feed(csv("foo", '"'))
    .feed(ssv("foo"))
    .feed(ssv("foo", '"'))
    .feed(tsv("foo"))
    .feed(tsv("foo", '"'))
    .feed(separatedValues("foo", '|'))
    .feed(separatedValues("foo", '|', '"'))
    .feed(jsonFile("foo"))
    .feed(jsonUrl("foo"))
    .feed(Iterator.from(0).map(i => Map("key" -> i)).take(10))
    .feed(() => Iterator.from(0).map(i => Map("key" -> i)).take(10))
    .feed(Array(Map.empty[String, Any], Map.empty[String, Any]).circular)
    .feed(IndexedSeq(Map.empty[String, Any]).circular)

  val records: Seq[Record[Any]] = csv("foo").readRecords
  val recordsCount: Int = csv("foo").recordsCount

  private val pauses = scenario("pauses")
    .pause(1)
    .pause(100.millis)
    .pause("#{pause}")
    .pause(session => 100.millis)
    .pause(1, 2)
    .pause(100.millis, 200.millis)
    .pause("#{min}", "#{max}")
    .pause(session => 100.millis, session => 200.millis)
    .pause(1, constantPauses)
    .pause(100.millis, constantPauses)
    .pause("#{pause}", constantPauses)
    .pause(session => 100.millis, constantPauses)
    .pause(1, 2, constantPauses)
    .pause(100.millis, 200.millis, constantPauses)
    .pause("#{min}", "#{max}", constantPauses)
    .pause(session => 100.millis, session => 200.millis, constantPauses)
    // pace
    .pace(1)
    .pace(1, "counter")
    .pace(1.second)
    .pace(1.second, "counter")
    .pace("#{pace}")
    .pace("#{pace}", "counter")
    .pace(session => 1.second)
    .pace(session => 1.second, "counter")
    .pace(1, 2)
    .pace(1, 2, "counter")
    .pace(1.second, 2.seconds)
    .pace(1.second, 2.seconds, "counter")
    .pace("#{min}", "#{max}", "counter")
    .pace(session => 1.second, session => 2.seconds)
    .pace(session => 1.second, session => 2.seconds, "counter")
    // rendezVous
    .rendezVous(5)

  private val loops = scenario("loops")
    // repeat
    .repeat(1)(chain1)
    .repeat(1, "counterName")(chain1)
    .repeat(session => 1)(chain1)
    .repeat(session => 1, "counterName")(chain1)
    //during - duration
    .during(1)(chain1)
    .during(1.second)(chain1)
    .during("#{duration}")(chain1)
    .during(session => 1.seconds)(chain1)
    //during - duration + counterName
    .during(1, "counterName")(chain1)
    .during(1.second, "counterName")(chain1)
    .during("#{duration}", "counterName")(chain1)
    .during(session => 1.seconds, "counterName")(chain1)
    //during - duration + exitASAP
    .during(1, exitASAP = true)(chain1)
    .during(1.second, exitASAP = true)(chain1)
    .during("#{duration}", exitASAP = true)(chain1)
    .during(session => 1.seconds, exitASAP = true)(chain1)
    //during - duration + counterName + exitASAP
    .during(1, "counterName", exitASAP = true)(chain1)
    .during(1.second, "counterName", exitASAP = true)(chain1)
    .during("#{duration}", "counterName", exitASAP = true)(chain1)
    .during(session => 1.seconds, "counterName", exitASAP = true)(chain1)
    // foreach
    .foreach(Seq(1), "attributeName")(chain1)
    .foreach(Seq(1), "attributeName", "counterName")(chain1)
    .foreach(session => Seq(1), "attributeName")(chain1)
    .foreach(session => Seq(1), "attributeName", "counterName")(chain1)
    // forever
    .forever(chain1)
    .forever("counterName")(chain1)
    // asLongAs
    .asLongAs("#{condition}")(chain1)
    .asLongAs("#{condition}", "counterName")(chain1)
    .asLongAs("#{condition}", exitASAP = true)(chain1)
    .asLongAs("#{condition}", "counterName", true)(chain1)
    .asLongAs(session => true)(chain1)
    .asLongAs(session => true, "counterName")(chain1)
    .asLongAs(session => true, exitASAP = true)(chain1)
    .asLongAs(session => true, "counterName", true)(chain1)
    // doWhile
    .doWhile("#{condition}")(chain1)
    .doWhile("#{condition}", "counterName")(chain1)
    .doWhile(session => true)(chain1)
    .doWhile(session => true, "counterName")(chain1)

  private val asLongAsDuringLoops = scenario("asLongAsDuring")
    .asLongAsDuring("#{condition}", "#{duration}")(chain1)
    .asLongAsDuring("#{condition}", "#{duration}", "counterName")(chain1)
    .asLongAsDuring("#{condition}", "#{duration}", exitASAP = true)(chain1)
    .asLongAsDuring("#{condition}", "#{duration}", "counterName", true)(chain1)
    .asLongAsDuring("#{condition}", 1.second)(chain1)
    .asLongAsDuring("#{condition}", 1.second, "counterName")(chain1)
    .asLongAsDuring("#{condition}", 1.second, exitASAP = true)(chain1)
    .asLongAsDuring("#{condition}", 1.second, "counterName", true)(chain1)
    .asLongAsDuring("#{condition}", session => 1.second)(chain1)
    .asLongAsDuring("#{condition}", session => 1.second, "counterName")(chain1)
    .asLongAsDuring("#{condition}", session => 1.second, exitASAP = true)(chain1)
    .asLongAsDuring("#{condition}", session => 1.second, "counterName", true)(chain1)
    .asLongAsDuring(session => true, "#{duration}")(chain1)
    .asLongAsDuring(session => true, "#{duration}", "counterName")(chain1)
    .asLongAsDuring(session => true, "#{duration}", exitASAP = true)(chain1)
    .asLongAsDuring(session => true, "#{duration}", "counterName", true)(chain1)
    .asLongAsDuring(session => true, 1.second)(chain1)
    .asLongAsDuring(session => true, 1.second, "counterName")(chain1)
    .asLongAsDuring(session => true, 1.second, exitASAP = true)(chain1)
    .asLongAsDuring(session => true, 1.second, "counterName", true)(chain1)
    .asLongAsDuring(session => true, session => 1.second)(chain1)
    .asLongAsDuring(session => true, session => 1.second, "counterName")(chain1)
    .asLongAsDuring(session => true, session => 1.second, exitASAP = true)(chain1)
    .asLongAsDuring(session => true, session => 1.second, "counterName", true)(chain1)

  private val doWhileDuringLoops = scenario("doWhileDuring")
    .doWhileDuring("#{condition}", "#{duration}")(chain1)
    .doWhileDuring("#{condition}", "#{duration}", "counterName")(chain1)
    .doWhileDuring("#{condition}", "#{duration}", exitASAP = true)(chain1)
    .doWhileDuring("#{condition}", "#{duration}", "counterName", true)(chain1)
    .doWhileDuring("#{condition}", 1.second)(chain1)
    .doWhileDuring("#{condition}", 1.second, "counterName")(chain1)
    .doWhileDuring("#{condition}", 1.second, exitASAP = true)(chain1)
    .doWhileDuring("#{condition}", 1.second, "counterName", true)(chain1)
    .doWhileDuring("#{condition}", session => 1.second)(chain1)
    .doWhileDuring("#{condition}", session => 1.second, "counterName")(chain1)
    .doWhileDuring("#{condition}", session => 1.second, exitASAP = true)(chain1)
    .doWhileDuring("#{condition}", session => 1.second, "counterName", true)(chain1)
    .doWhileDuring(session => true, "#{duration}")(chain1)
    .doWhileDuring(session => true, "#{duration}", "counterName")(chain1)
    .doWhileDuring(session => true, "#{duration}", exitASAP = true)(chain1)
    .doWhileDuring(session => true, "#{duration}", "counterName", true)(chain1)
    .doWhileDuring(session => true, 1.second)(chain1)
    .doWhileDuring(session => true, 1.second, "counterName")(chain1)
    .doWhileDuring(session => true, 1.second, exitASAP = true)(chain1)
    .doWhileDuring(session => true, 1.second, "counterName", true)(chain1)
    .doWhileDuring(session => true, session => 1.second)(chain1)
    .doWhileDuring(session => true, session => 1.second, "counterName")(chain1)
    .doWhileDuring(session => true, session => 1.second, exitASAP = true)(chain1)
    .doWhileDuring(session => true, session => 1.second, "counterName", true)(chain1)

  private val doIfs = scenario("doIf")
    .doIf("#{condition}")(chain1)
    .doIf(session => true)(chain1)
    // doIfOrElse
    .doIfOrElse("#{condition}")(chain1)(chain2)
    .doIfOrElse(session => true)(chain1)(chain2)
    // doIfEquals
    .doIfEquals("#{actual}", 1)(chain1)
    .doIfEquals("#{actual}", "#{expected}")(chain1)
    .doIfEquals("#{actual}", session => 1)(chain1)
    .doIfEquals(session => "actual", 1)(chain1)
    .doIfEquals(session => "actual", "#{expected}")(chain1)
    .doIfEquals(session => "actual", session => 1)(chain1)
    // doIfEqualsOrElse
    .doIfEqualsOrElse("#{actual}", 1)(chain1)(chain2)
    .doIfEqualsOrElse("#{actual}", "#{expected}")(chain1)(chain2)
    .doIfEqualsOrElse("#{actual}", session => 1)(chain1)(chain2)
    .doIfEqualsOrElse(session => "actual", 1)(chain1)(chain2)
    .doIfEqualsOrElse(session => "actual", "#{expected}")(chain1)(chain2)
    .doIfEqualsOrElse(session => "actual", session => 1)(chain1)(chain2)
    // doSwitch
    .doSwitch("#{value}")(
      "value1" -> chain1,
      "value2" -> chain2
    )
    .doSwitch(session => "value")(
      "value1" -> chain1,
      "value2" -> chain2
    )
    // doSwitchOrElse
    .doSwitchOrElse("#{value}")(
      "value1" -> chain1,
      "value2" -> chain2
    )(chain2)
    .doSwitchOrElse(session => "value")(
      "value1" -> chain1,
      "value2" -> chain2
    )(chain2)
    // randomSwitch
    .randomSwitch(
      50.0 -> chain1,
      50.0 -> chain2
    )
    // randomSwitchOrElse
    .randomSwitchOrElse(
      50.0 -> chain1,
      50.0 -> chain2
    )(chain2)
    // uniformRandomSwitch
    .uniformRandomSwitch(
      chain1,
      chain2
    )
    // roundRobinSwitch
    .roundRobinSwitch(
      chain1,
      chain2
    )
  private val exits = scenario("exits")
    // exitBlockOnFail
    .exitBlockOnFail(chain1)
    // tryMax
    .tryMax(1)(chain1)
    .tryMax(1, "counterName")(chain1)
    .tryMax("#{times}")(chain1)
    .tryMax("#{times}", "counterName")(chain1)
    .tryMax(session => 1)(chain1)
    .tryMax(session => 1, "counterName")(chain1)
    // exitHereIf
    .exitHereIf("#{condition}")
    .exitHereIf(session => true)
    // exitHere
    .exitHere
    // exitHereIfFailed
    .exitHereIfFailed
    // stopInjector
    .stopInjector("#{message}")
    .stopInjector(session => "message")

  registerPebbleExtensions(null.asInstanceOf[com.mitchellbosecke.pebble.extension.Extension])
  setUp(
    scn.inject(
      rampUsers(5).during(1),
      rampUsers(5).during(1.second),
      stressPeakUsers(5).during(1),
      stressPeakUsers(5).during(1.second),
      atOnceUsers(1000),
      constantUsersPerSec(10).during(1),
      constantUsersPerSec(10).during(1.second),
      rampUsersPerSec(100).to(200).during(1),
      rampUsersPerSec(100).to(200).during(1.second),
      nothingFor(1),
      nothingFor(1.second),
      incrementUsersPerSec(1.0).times(5).eachLevelLasting(1),
      incrementUsersPerSec(1.0).times(5).eachLevelLasting(1).startingFrom(1.0),
      incrementUsersPerSec(1.0).times(5).eachLevelLasting(1).separatedByRampsLasting(1),
      incrementUsersPerSec(1.0)
        .times(5)
        .eachLevelLasting(1)
        .startingFrom(1.0)
        .separatedByRampsLasting(1),
      incrementUsersPerSec(1.0)
        .times(5)
        .eachLevelLasting(1.second)
        .startingFrom(1.0)
        .separatedByRampsLasting(1.second)
    ),
    scn
      .inject(
        constantConcurrentUsers(100).during(1),
        constantConcurrentUsers(100).during(1.second),
        rampConcurrentUsers(1).to(5).during(1),
        rampConcurrentUsers(1).to(5).during(1.second),
        incrementConcurrentUsers(1).times(5).eachLevelLasting(1),
        incrementConcurrentUsers(1).times(5).eachLevelLasting(1),
        incrementConcurrentUsers(1).times(5).eachLevelLasting(1).startingFrom(1),
        incrementConcurrentUsers(1)
          .times(5)
          .eachLevelLasting(1)
          .separatedByRampsLasting(1),
        incrementConcurrentUsers(1)
          .times(5)
          .eachLevelLasting(1)
          .startingFrom(1)
          .separatedByRampsLasting(1),
        incrementConcurrentUsers(1)
          .times(5)
          .eachLevelLasting(1.second)
          .startingFrom(1)
          .separatedByRampsLasting(1.second)
      )
      .andThen(scn.inject(atOnceUsers(1)))
  )
    .assertions(
      global.allRequests.count.is(5L),
      global.allRequests.percent.is(5.5),
      forAll.allRequests.count.is(5L),
      details("group" / "request").allRequests.count.is(5L)
    )
    .maxDuration(1)
    .maxDuration(1.second)
    .throttle(reachRps(100).in(1), reachRps(100).in(1.second), jumpToRps(100), holdFor(1), holdFor(1.second))
    .disablePauses
    .constantPauses
    .exponentialPauses
    .customPauses(session => 1L)
    .uniformPauses(1)
    .uniformPauses(1.second)
}

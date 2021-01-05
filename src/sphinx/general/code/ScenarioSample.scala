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
import io.gatling.core.session.Session
import io.gatling.http.Predef._

class ScenarioSample {

  //#bootstrapping
  scenario("My Scenario")
  //#bootstrapping

  //#exec-example
  scenario("My Scenario")
    .exec(http("Get Homepage").get("http://github.com/gatling/gatling"))
  //#exec-example

  //#session-lambda
  exec { session =>
    // displays the content of the session in the console (debugging only)
    println(session)

    // return the original session
    session
  }

  exec { session =>
    // return a new session instance with a new "foo" attribute whose value is "bar"
    session.set("foo", "bar")
  }
  //#session-lambda

  def someSessionBasedCondition(session: Session): Boolean = true

  //#session-improper
  exec { session =>
    if (someSessionBasedCondition(session)) {
      // just create a builder that is immediately discarded, hence doesn't do anything
      // you should be using a doIf here
      http("Get Homepage").get("http://github.com/gatling/gatling")
    }
    session
  }
  //#session-improper

  //#flattenMapIntoAttributes
  // assuming the Session contains an attribute named "theMap" whose content is :
  // Map("foo" -> "bar", "baz" -> "qix")

  exec(flattenMapIntoAttributes("${theMap}"))

  // the Session contains 2 new attributes "foo" and "baz".
  //#flattenMapIntoAttributes

  //#pace
  forever(
    pace(5.seconds)
      .exec(
        pause(1.second, 4.seconds) // Will be run every 5 seconds, irrespective of what pause time is used
      )
  )
  //#pace

  val times = 4
  val counterName, sequenceName, elementName = "foo"
  val myChain = exec(Session.Identity(_))
  val condition, exitASAP = true
  val duration = 5.seconds

  //#repeat-example
  repeat(times, counterName) {
    myChain
  }
  //#repeat-example

  //#repeat-variants
  repeat(20) { myChain } // will loop on myChain 20 times
  repeat("${myKey}") { myChain } // will loop on myChain (Int value of the Session attribute myKey) times
  repeat(session => session("foo").as[Int] /* or anything that returns an Int*/ ) { myChain }
  //#repeat-variants

  //#foreach
  foreach(sequenceName, elementName, counterName) {
    myChain
  }
  //#foreach

  //#during
  during(duration, counterName, exitASAP) {
    myChain
  }
  //#during

  //#asLongAs
  asLongAs(condition, counterName, exitASAP) {
    myChain
  }
  //#asLongAs

  //#doWhile
  doWhile(condition, counterName) {
    myChain
  }
  //#doWhile

  //#asLongAsDuring
  asLongAsDuring(condition, duration, counterName) {
    myChain
  }
  //#asLongAsDuring

  //#doWhileDuring
  doWhileDuring(condition, duration, counterName) {
    myChain
  }
  //#doWhileDuring

  //#forever
  forever(counterName) {
    myChain
  }
  //#forever

  //#doIf
  doIf("${myBoolean}") {
    // executed if the session value stored in "myBoolean" is true
    exec(http("...").get("..."))
  }
  //#doIf

  //#doIf-session
  doIf(session => session("myKey").as[String].startsWith("admin")) {
    // executed if the session value stored in "myKey" starts with "admin"
    exec(http("if true").get("..."))
  }
  //#doIf-session

  //#doIfEquals
  doIfEquals("${actualValue}", "expectedValue") {
    // executed if the session value stored in "actualValue" is equal to "expectedValue"
    exec(http("...").get("..."))
  }
  //#doIfEquals

  //#doIfOrElse
  doIfOrElse(session => session("myKey").as[String].startsWith("admin")) {
    // executed if the session value stored in "myKey" starts with "admin"
    exec(http("if true").get("..."))
  } {
    // executed if the session value stored in "myKey" does not start with "admin"
    exec(http("if false").get("..."))
  }
  //#doIfOrElse

  //#doIfEqualsOrElse
  doIfEqualsOrElse(session => session("actualValue").as[String], "expectedValue") {
    // executed if the session value stored in "actualValue" equals to "expectedValue"
    exec(http("if true").get("..."))
  } {
    // executed if the session value stored in "actualValue" is not equal to "expectedValue"
    exec(http("if false").get("..."))
  }
  //#doIfEqualsOrElse

  val chain1, chain2, myFallbackChain = myChain
  val key1, key2 = "foo"
  val percentage1, percentage2 = .50

  //#doSwitch
  doSwitch("${myKey}")( // beware: use parentheses, not curly braces!
    key1 -> chain1,
    key2 -> chain2
  )
  //#doSwitch

  //#doSwitchOrElse
  doSwitchOrElse("${myKey}")( // beware: use parentheses, not curly braces!
    key1 -> chain1,
    key2 -> chain2
  )(
    myFallbackChain
  )
  //#doSwitchOrElse

  //#randomSwitch
  randomSwitch( // beware: use parentheses, not curly braces!
    percentage1 -> chain1,
    percentage2 -> chain2
  )
  //#randomSwitch

  //#randomSwitchOrElse
  randomSwitchOrElse( // beware: use parentheses, not curly braces!
    percentage1 -> chain1,
    percentage2 -> chain2
  ) {
    myFallbackChain
  }
  //#randomSwitchOrElse

  //#uniformRandomSwitch
  uniformRandomSwitch( // beware: use parentheses, not curly braces!
    chain1,
    chain2
  )
  //#uniformRandomSwitch

  //#roundRobinSwitch
  roundRobinSwitch( // beware: use parentheses, not curly braces!
    chain1,
    chain2
  )
  //#roundRobinSwitch

  //#tryMax
  tryMax(times, counterName) {
    myChain
  }
  //#tryMax

  //#exitBlockOnFail
  exitBlockOnFail {
    myChain
  }
  //#exitBlockOnFail

  //#exitHereIf
  exitHere
  //#exitHereIf

  //#exitHereIf
  exitHereIf("${myBoolean}")
  exitHereIf(session => true)
  //#exitHereIf

  //#exitHereIfFailed
  exitHereIfFailed
  //#exitHereIfFailed

  val groupName = "foo"
  //#group
  group(groupName) {
    myChain
  }
  //#group

  val scn = scenario("foo")
  val httpProtocol = http

  //#protocol
  scn.inject(atOnceUsers(5)).protocols(httpProtocol)
  //#protocol

  //#throttling
  scn.inject(rampUsers(500).during(10.minutes)).throttle(reachRps(100).in(10.seconds), holdFor(10.minutes))
  //#throttling
}

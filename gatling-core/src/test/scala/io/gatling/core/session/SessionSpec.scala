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

package io.gatling.core.session

import io.gatling.BaseSpec
import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.validation.{ Failure, Success }
import io.gatling.core.EmptySession
import io.gatling.core.action.Action

class SessionSpec extends BaseSpec with EmptySession {
  private val nextAction = mock[Action]

  "setAll" should "set all give key/values pairs in session" in {
    val session = emptySession.setAll("key" -> 1, "otherKey" -> 2)
    session.attributes.contains("key") shouldBe true
    session.attributes.contains("otherKey") shouldBe true
    session.attributes("key") shouldBe 1
    session.attributes("otherKey") shouldBe 2
  }

  "reset" should "remove all non private attributes from the session" in {
    val privateAttribute = SessionPrivateAttributes.PrivateAttributePrefix + "foo"
    val session = emptySession
      .setAll("key" -> 1, "otherKey" -> 2, privateAttribute -> 5)
      .reset

    session.attributes shouldBe Map(privateAttribute -> 5)
  }

  it should "preserve loop counter" in {
    val session = emptySession
      .set("bar", 5)
      .enterLoop(counterName = "foo", condition = _ => Success(true), exitAction = null, exitASAP = false)
      .reset

    session.attributes.get("foo") shouldBe Some(0)
    session.attributes.get("bar") shouldBe None
  }

  it should "preserve loop timestamp" in {
    val session = emptySession
      .set("bar", 5)
      .enterTimeBasedLoop(counterName = "foo", condition = _ => Success(true), exitAction = null, exitASAP = false, nowMillis = 1000)
      .reset

    session.attributes.get("foo") shouldBe Some(0)
    session.attributes.get(Session.timestampName("foo")) shouldBe Some(1000)
    session.attributes.get("bar") shouldBe None
  }

  "remove" should "remove an attribute from the session if present" in {
    val session = emptySession.set("key", "value").remove("key")
    session.attributes.contains("key") shouldBe false
  }

  it should "return the current session if the attribute is not in session" in {
    val session = emptySession.set("key", "value")
    val unmodifiedSession = session.remove("otherKey")
    session should be theSameInstanceAs unmodifiedSession
  }

  "removeAll" should "remove all specified attributes from the session if present" in {
    val session = emptySession.set("key", "value").set("otherKey", "otherValue").removeAll("key", "otherKey")
    session.attributes.contains("key") shouldBe false
    session.attributes.contains("otherKey") shouldBe false
  }

  it should "return the current session if the specified attributes are not in the session" in {
    val session = emptySession.set("key", "value").set("otherKey", "otherValue")
    val unmodifiedSession = session.removeAll("unknownKey", "otherUnknownKey")
    session should be theSameInstanceAs unmodifiedSession
  }

  "contains" should "return true if the attribute is in the session" in {
    val session = emptySession.set("key", "value")
    session.contains("key") shouldBe true
  }

  it should "return false if the attribute is not in the session" in {
    emptySession.contains("foo") shouldBe false
  }

  "loopCounterValue" should "return a counter stored in the session as an Int" in {
    val session = emptySession.set("counter", 1)
    session.loopCounterValue("counter") shouldBe 1
  }

  "loopTimestampValue" should "return a counter stored in the session as an Int" in {
    val timestamp = System.currentTimeMillis()
    val session = emptySession.set("timestamp.foo", timestamp)
    session.loopTimestampValue("foo") shouldBe timestamp
  }

  "enterGroup" should "add a 'root' group block is there is no group in the stack" in {
    val session = emptySession
    val sessionWithGroup = session.enterGroup("root group", System.currentTimeMillis())
    val lastBlock = sessionWithGroup.blockStack.head
    lastBlock shouldBe a[GroupBlock]
    lastBlock.asInstanceOf[GroupBlock].groups shouldBe List("root group")
  }

  it should "add a group block with its hierarchy is there are groups in the stack" in {
    val session = emptySession.enterGroup("root group", System.currentTimeMillis()).enterGroup("child group", System.currentTimeMillis())
    val sessionWithThreeGroups = session.enterGroup("last group", System.currentTimeMillis())
    val lastBlock = sessionWithThreeGroups.blockStack.head
    lastBlock shouldBe a[GroupBlock]
    lastBlock.asInstanceOf[GroupBlock].groups shouldBe List("root group", "child group", "last group")
  }

  "exitGroup" should "remove the GroupBlock from the stack if it's on top of the stack" in {
    val session = emptySession.enterGroup("root group", System.currentTimeMillis())
    val sessionWithoutGroup = session.exitGroup(session.blockStack.tail)
    sessionWithoutGroup.blockStack shouldBe empty
  }

  "logGroupRequestTimings" should "update stats in all parent groups" in {
    val session = emptySession
      .enterGroup("root group", System.currentTimeMillis())
      .enterGroup("child group", System.currentTimeMillis())
      .enterTryMax("tryMax", nextAction)
    val sessionWithGroupStatsUpdated = session.logGroupRequestTimings(1, 6)
    val allGroupBlocks = sessionWithGroupStatsUpdated.blockStack.collect { case g: GroupBlock => g }

    for (group <- allGroupBlocks) {
      group.cumulatedResponseTime shouldBe 5
    }
  }

  it should "leave the session unmodified if there is no groups in the stack" in {
    val session = emptySession
    val unModifiedSession = session.logGroupRequestTimings(1, 2)

    session should be theSameInstanceAs unModifiedSession
  }

  "groupHierarchy" should "return the group hierarchy if there is one" in {
    val session = emptySession
    session.groups shouldBe empty

    val sessionWithGroup = session
      .enterGroup("root group", System.currentTimeMillis())
      .enterGroup("child group", System.currentTimeMillis())
    sessionWithGroup.groups shouldBe List("root group", "child group")
  }

  "enterTryMax" should "add a TryMaxBlock on top of the stack and init a counter" in {
    val session = emptySession.enterTryMax("tryMax", nextAction)

    session.blockStack.head shouldBe a[TryMaxBlock]
    session.contains("tryMax") shouldBe true
  }

  "exitTryMax" should "simply exit the closest TryMaxBlock and remove its associated counter if it has not failed" in {
    val session = emptySession.enterTryMax("tryMax", nextAction).exitTryMax

    session.blockStack shouldBe empty
    session.contains("tryMax") shouldBe false
  }

  it should "simply exit the TryMaxBlock and remove its associated counter if it has failed but with no other TryMaxBlock in the stack" in {
    val session = emptySession
      .enterGroup("root group", System.currentTimeMillis())
      .enterTryMax("tryMax", nextAction)
      .markAsFailed
      .exitTryMax

    session.blockStack.head shouldBe a[GroupBlock]
    session.contains("tryMax") shouldBe false
  }

  it should "exit the TryMaxBlock, remove its associated counter and set the closest TryMaxBlock in the stack's status to KO if it has failed" in {
    val session = emptySession
      .enterTryMax("tryMax1", nextAction)
      .enterGroup("root group", System.currentTimeMillis())
      .enterTryMax("tryMax2", nextAction)
      .markAsFailed
      .exitTryMax

    session.blockStack.head shouldBe a[GroupBlock]
    session.blockStack(1) shouldBe a[TryMaxBlock]
    session.blockStack(1).asInstanceOf[TryMaxBlock].status shouldBe KO
    session.contains("tryMax2") shouldBe false
  }

  it should "leave the session unmodified if there is no TryMaxBlock on top of the stack" in {
    val session = emptySession
    val unmodifiedSession = session.exitTryMax

    session should be theSameInstanceAs unmodifiedSession
  }

  it should "propagate the failure to the baseStatus" in {
    val session = emptySession
      .enterTryMax("tryMax1", nextAction)
      .markAsFailed
      .exitTryMax

    session.isFailed shouldBe true
  }

  "isFailed" should "return true if baseStatus is KO and there is no failed TryMaxBlock in the stack" in {
    val session = emptySession.copy(baseStatus = KO)

    session.isFailed shouldBe true
  }

  it should "return true if baseStatus is OK and there is a failed TryMaxBlock in the stack" in {
    val session = emptySession.copy(blockStack = List(TryMaxBlock("tryMax", nextAction, KO)))

    session.isFailed shouldBe true
  }

  it should "return false if baseStatus is OK and there is no TryMaxBlock in the stack" in {
    emptySession.isFailed shouldBe false
  }

  it should "return false if baseStatus is OK and there is no failed TryMaxBlock in the stack" in {
    val session = emptySession.copy(blockStack = List(TryMaxBlock("tryMax", nextAction, OK)))

    session.isFailed shouldBe false
  }

  "status" should "be OK if the session is not failed" in {
    emptySession.status shouldBe OK
  }

  it should "be KO if the session is failed" in {
    emptySession.copy(baseStatus = KO).status shouldBe KO
  }

  "markAsSucceeded" should "only set the baseStatus to OK if it was originally KO and there is no TryMaxBlock in the stack" in {
    val session = emptySession.copy(baseStatus = KO)
    val failedSession = session.markAsSucceeded

    failedSession should not be theSameInstanceAs(session)
    failedSession.baseStatus shouldBe OK
  }

  it should "leave the session unmodified if baseStatus is already OK and there is no TryMaxBlock in the stack" in {
    val session = emptySession
    val failedSession = session.markAsSucceeded

    failedSession should be theSameInstanceAs session
  }

  it should "set the TryMaxBlock's status to OK if there is a TryMaxBlock in the stack, but leave the baseStatus unmodified" in {
    val session = emptySession
      .copy(baseStatus = KO)
      .enterGroup("root group", System.currentTimeMillis())
      .enterTryMax("tryMax", nextAction)
    val failedSession = session.markAsSucceeded

    failedSession.baseStatus shouldBe KO
    failedSession.blockStack.head.asInstanceOf[TryMaxBlock].status shouldBe OK
  }

  "markAsFailed" should "only set the baseStatus to KO if it was not set and there is no TryMaxBlock in the stack" in {
    val session = emptySession
    val failedSession = session.markAsFailed

    failedSession should not be theSameInstanceAs(session)
    failedSession.baseStatus shouldBe KO
  }

  it should "leave the session unmodified if baseStatus is already KO and the stack is empty" in {
    val session = emptySession.copy(baseStatus = KO)
    val failedSession = session.markAsFailed

    failedSession should be theSameInstanceAs session
  }

  it should "set the TryMaxBlock's status to KO if there is a TryMaxBlock in the stack, but leave the baseStatus unmodified" in {
    val session = emptySession
      .enterGroup("root group", System.currentTimeMillis())
      .enterTryMax("tryMax", nextAction)
    val failedSession = session.markAsFailed

    failedSession.baseStatus shouldBe OK
    failedSession.blockStack.head.asInstanceOf[TryMaxBlock].status shouldBe KO
  }

  "enterLoop" should "add an ExitASAPLoopBlock on top of the stack and init a counter when exitASAP = true" in {
    val session = emptySession
      .enterLoop("loop", true.expressionSuccess, nextAction, exitASAP = true)

    session.blockStack.head shouldBe a[ExitAsapLoopBlock]
    session.contains("loop") shouldBe true
    session.attributes("loop") shouldBe 0
  }

  it should "add an ExitOnCompleteLoopBlock on top of the stack and init a counter when exitASAP = false" in {
    val session = emptySession
      .enterLoop("loop", true.expressionSuccess, nextAction, exitASAP = false)

    session.blockStack.head shouldBe a[ExitOnCompleteLoopBlock]
    session.contains("loop") shouldBe true
    session.attributes("loop") shouldBe 0
  }

  "exitLoop" should "remove the LoopBlock from the top of the stack and its associated counter" in {
    val session = emptySession
      .enterLoop("loop", true.expressionSuccess, nextAction, exitASAP = false)
    val sessionOutOfLoop = session.exitLoop

    sessionOutOfLoop.blockStack shouldBe empty
    sessionOutOfLoop.contains("loop") shouldBe false
  }

  it should "leave the stack unmodified if there's no LoopBlock on top of the stack" in {
    val session = emptySession
    val unModifiedSession = session.exitLoop
    session should be theSameInstanceAs unModifiedSession
  }

  "enterTimeBasedLoop" should "add a counter, initialized to 0, and a timestamp for the counter creation in the session" in {
    val session = emptySession.enterTimeBasedLoop("counter", _ => Success(true), null, exitASAP = false, System.currentTimeMillis())

    session.contains("counter") shouldBe true
    session.attributes("counter") shouldBe 0
    session.contains("timestamp.counter") shouldBe true
  }

  "incrementCounter" should "increment a counter in session" in {
    val session = emptySession.enterLoop("counter", _ => Success(true), null, exitASAP = false)
    val sessionWithUpdatedCounter = session.incrementCounter("counter")

    sessionWithUpdatedCounter.attributes("counter") shouldBe 1
  }

  it should "should leave the session unmodified if there was no counter created with the specified name" in {
    val session = emptySession
    val unModifiedSession = session.incrementCounter("counter")
    session should be theSameInstanceAs unModifiedSession
  }

  "removeCounter" should "remove a counter and its associated timestamp from the session" in {
    val session = emptySession.enterTimeBasedLoop("counter", _ => Success(true), null, exitASAP = false, System.currentTimeMillis())
    val sessionWithRemovedCounter = session.removeCounter("counter")

    sessionWithRemovedCounter.contains("counter") shouldBe false
    sessionWithRemovedCounter.contains("timestamp.counter") shouldBe false
  }

  it should "should leave the session unmodified if there was no counter created with the specified name" in {
    val session = emptySession
    val unModifiedSession = session.removeCounter("counter")
    session should be theSameInstanceAs unModifiedSession
  }

  "terminate" should "call the userEnd function" in {
    var i = 0
    val session = emptySession.copy(onExit = _ => i += 1)
    session.exit()

    i shouldBe 1
  }

  "MarkAsFailedUpdate function" should "mark as failed the session passed as parameter" in {
    val failedSession = Session.MarkAsFailedUpdate(emptySession)
    failedSession.isFailed shouldBe true
  }

  "Identity function" should "return the same session instance" in {
    val session = emptySession
    val unModifiedSession = Session.Identity(session)
    session should be theSameInstanceAs unModifiedSession
  }

  "as[T]" should "return the value when key is defined and value of the expected type" in {
    val session = emptySession.set("foo", "bar")
    session("foo").as[String] shouldBe "bar"
  }

  it should "support parsing a valid String into an Int" in {
    val session = emptySession.set("foo", "200")
    session("foo").as[String] shouldBe "200"
    session("foo").as[Int] shouldBe 200
  }

  it should "throw an exception when key isn't defined" in {
    val session = emptySession
    a[java.util.NoSuchElementException] shouldBe thrownBy(session("foo").as[String])
  }

  it should "throw an IllegalArgumentException when the String value can't be parsed" in {
    val session = emptySession.set("foo", "bar")
    a[IllegalArgumentException] shouldBe thrownBy(session("foo").as[Int])
  }

  it should "throw a ClassCastException when the value can't be turned into the expected type" in {
    val session = emptySession.set("foo", true)
    a[ClassCastException] shouldBe thrownBy(session("foo").as[Int])
  }

  "asOption[T]" should "return a Some(value) when key is defined and value of the expected type" in {
    val session = emptySession.set("foo", "bar")
    session("foo").asOption[String] shouldBe Some("bar")
  }

  it should "support parsing a valid String into an Int" in {
    val session = emptySession.set("foo", "200")
    session("foo").asOption[String] shouldBe Some("200")
    session("foo").asOption[Int] shouldBe Some(200)
  }

  it should "return None when key isn't defined" in {
    val session = emptySession
    session("foo").asOption[String] shouldBe None
  }

  it should "throw a IllegalArgumentException when the String value can't be parsed" in {
    val session = emptySession.set("foo", "bar")
    a[IllegalArgumentException] shouldBe thrownBy(session("foo").asOption[Int])
  }

  it should "throw a ClassCastException when the value isn't of the expected type" in {
    val session = emptySession.set("foo", true)
    a[ClassCastException] shouldBe thrownBy(session("foo").asOption[Int])
  }

  "validate[T]" should "return a Validation(value) when key is defined and value of the expected type" in {
    val session = emptySession.set("foo", "bar")
    session("foo").validate[String] shouldBe Success("bar")
  }

  it should "support parsing a valid String into an Int" in {
    val session = emptySession.set("foo", "200")
    session("foo").validate[String] shouldBe Success("200")
    session("foo").validate[Int] shouldBe Success(200)
  }

  it should "return a Failure when key isn't defined" in {
    val session = emptySession
    session("foo").validate[String] shouldBe a[Failure]
  }

  it should "return a Failure when the value isn't of the expected type" in {
    val session = emptySession.set("foo", "bar")
    session("foo").validate[Int] shouldBe a[Failure]
  }
}

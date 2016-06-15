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
package io.gatling.core.session

import io.gatling.BaseSpec
import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.TimeHelper._
import io.gatling.commons.validation.{ Failure, Success }
import io.gatling.core.action.Action

class SessionSpec extends BaseSpec {

  val nextAction = mock[Action]

  def newSession = Session("scenario", 0)

  "setAll" should "set all give key/values pairs in session" in {
    val session = newSession.setAll("key" -> 1, "otherKey" -> 2)
    session.attributes.contains("key") shouldBe true
    session.attributes.contains("otherKey") shouldBe true
    session.attributes("key") shouldBe 1
    session.attributes("otherKey") shouldBe 2
  }

  "reset" should "remove all attributes from the session" in {
    val session = newSession.setAll("key" -> 1, "otherKey" -> 2)
    val resetSession = session.reset
    resetSession.attributes shouldBe empty
  }

  "remove" should "remove an attribute from the session if present" in {
    val session = newSession.set("key", "value").remove("key")
    session.attributes.contains("key") shouldBe false
  }

  it should "return the current session if the attribute is not in session" in {
    val session = newSession.set("key", "value")
    val unmodifiedSession = session.remove("otherKey")
    session should be theSameInstanceAs unmodifiedSession
  }

  "removeAll" should "remove all specified attributes from the session if present" in {
    val session = newSession.set("key", "value").set("otherKey", "otherValue").removeAll("key", "otherKey")
    session.attributes.contains("key") shouldBe false
    session.attributes.contains("otherKey") shouldBe false
  }

  it should "return the current session if the specified attributes are not in the session" in {
    val session = newSession.set("key", "value").set("otherKey", "otherValue")
    val unmodifiedSession = session.removeAll("unknownKey", "otherUnknownKey")
    session should be theSameInstanceAs unmodifiedSession
  }

  "contains" should "return true if the attribute is in the session" in {
    val session = newSession.set("key", "value")
    session.contains("key") shouldBe true
  }

  it should "return false if the attribute is not in the session" in {
    newSession.contains("foo") shouldBe false
  }

  "setDrift" should "set the drift" in {
    newSession.setDrift(10).drift shouldBe 10
  }

  "increaseDrift" should "increase drift by the specified amount" in {
    val session = newSession.setDrift(10)
    session.increaseDrift(10).drift shouldBe 20
  }

  "loopCounterValue" should "return a counter stored in the session as an Int" in {
    val session = newSession.set("counter", 1)
    session.loopCounterValue("counter") shouldBe 1
  }

  "loopTimestampValue" should "return a counter stored in the session as an Int" in {
    val timestamp = nowMillis
    val session = newSession.set("timestamp.foo", timestamp)
    session.loopTimestampValue("foo") shouldBe timestamp
  }

  "enterGroup" should "add a 'root' group block is there is no group in the stack" in {
    val session = newSession
    val sessionWithGroup = session.enterGroup("root group")
    val lastBlock = sessionWithGroup.blockStack.head
    lastBlock shouldBe a[GroupBlock]
    lastBlock.asInstanceOf[GroupBlock].hierarchy shouldBe List("root group")
  }

  it should "add a group block with its hierarchy is there are groups in the stack" in {
    val session = newSession.enterGroup("root group").enterGroup("child group")
    val sessionWithThreeGroups = session.enterGroup("last group")
    val lastBlock = sessionWithThreeGroups.blockStack.head
    lastBlock shouldBe a[GroupBlock]
    lastBlock.asInstanceOf[GroupBlock].hierarchy shouldBe List("root group", "child group", "last group")
  }

  "exitGroup" should "remove the GroupBlock from the stack if it's on top of the stack" in {
    val session = newSession.enterGroup("root group")
    val sessionWithoutGroup = session.exitGroup
    sessionWithoutGroup.blockStack shouldBe empty
  }

  it should "leave the stack unmodified if there's no GroupBlock on top of the stack" in {
    val session = newSession
    val unModifiedSession = session.exitGroup
    session should be theSameInstanceAs unModifiedSession
  }

  "logGroupAsyncRequests" should "update stats in all parent groups" in {
    val session = newSession.enterGroup("root group").enterGroup("child group").enterTryMax("tryMax", nextAction)
    val sessionWithGroupStatsUpdated = session.logGroupRequest(5, KO)
    val allGroupBlocks = sessionWithGroupStatsUpdated.blockStack.collect { case g: GroupBlock => g }

    for (group <- allGroupBlocks) {
      group.cumulatedResponseTime shouldBe 5
      group.status shouldBe KO
    }
  }

  it should "leave the session unmodified if there is no groups in the stack" in {
    val session = newSession
    val unModifiedSession = session.logGroupRequest(1, KO)

    session should be theSameInstanceAs unModifiedSession
  }

  "logGroupRequest" should "add the response time to all parents groups" in {
    val session = newSession.enterGroup("root group").enterGroup("child group").enterTryMax("tryMax", nextAction)
    val sessionWithGroupStatsUpdated = session.logGroupRequest(5, OK)
    val allGroupBlocks = sessionWithGroupStatsUpdated.blockStack.collect { case g: GroupBlock => g }

    for (group <- allGroupBlocks) {
      group.cumulatedResponseTime shouldBe 5
      group.status shouldBe OK
    }
  }

  it should "add the response time to all parents groups and add KO all if status was KO" in {
    val session = newSession.enterGroup("root group").enterGroup("child group").enterTryMax("tryMax", nextAction)
    val sessionWithGroupStatsUpdated = session.logGroupRequest(5, KO)
    val allGroupBlocks = sessionWithGroupStatsUpdated.blockStack.collect { case g: GroupBlock => g }

    for (group <- allGroupBlocks) {
      group.cumulatedResponseTime shouldBe 5
      group.status shouldBe KO
    }
  }

  it should "leave the session unmodified if there is no groups in the stack" in {
    val session = newSession
    val unModifiedSession = session.logGroupRequest(1, KO)

    session should be theSameInstanceAs unModifiedSession
  }

  "groupHierarchy" should "return the group hierarchy if there is one" in {
    val session = newSession
    session.groupHierarchy shouldBe empty

    val sessionWithGroup = session.enterGroup("root group").enterGroup("child group")
    sessionWithGroup.groupHierarchy shouldBe List("root group", "child group")
  }

  "enterTryMax" should "add a TryMaxBlock on top of the stack and init a counter" in {
    val session = newSession.enterTryMax("tryMax", nextAction)

    session.blockStack.head shouldBe a[TryMaxBlock]
    session.contains("tryMax") shouldBe true
  }

  "exitTryMax" should "simply exit the closest TryMaxBlock and remove its associated counter if it has not failed" in {
    val session = newSession.enterTryMax("tryMax", nextAction).exitTryMax

    session.blockStack shouldBe empty
    session.contains("tryMax") shouldBe false
  }

  it should "simply exit the TryMaxBlock and remove its associated counter if it has failed but with no other TryMaxBlock in the stack" in {
    val session = newSession.enterGroup("root group").enterTryMax("tryMax", nextAction).markAsFailed.exitTryMax

    session.blockStack.head shouldBe a[GroupBlock]
    session.contains("tryMax") shouldBe false
  }

  it should "exit the TryMaxBlock, remove its associated counter and set the closest TryMaxBlock in the stack's status to KO if it has failed" in {
    val session = newSession.enterTryMax("tryMax1", nextAction).enterGroup("root group").enterTryMax("tryMax2", nextAction).markAsFailed.exitTryMax

    session.blockStack.head shouldBe a[GroupBlock]
    session.blockStack(1) shouldBe a[TryMaxBlock]
    session.blockStack(1).asInstanceOf[TryMaxBlock].status shouldBe KO
    session.contains("tryMax2") shouldBe false
  }

  it should "leave the session unmodified if there is no TryMaxBlock on top of the stack" in {
    val session = newSession
    val unmodifiedSession = session.exitTryMax

    session should be theSameInstanceAs unmodifiedSession
  }

  it should "propagate the failure to the baseStatus" in {
    val session = newSession.enterTryMax("tryMax1", nextAction).markAsFailed.exitTryMax

    session.isFailed shouldBe true
  }

  "isFailed" should "return true if baseStatus is KO and there is no failed TryMaxBlock in the stack" in {
    val session = newSession.copy(baseStatus = KO)

    session.isFailed shouldBe true
  }

  it should "return true if baseStatus is OK and there is a failed TryMaxBlock in the stack" in {
    val session = newSession.copy(blockStack = List(TryMaxBlock("tryMax", nextAction, status = KO)))

    session.isFailed shouldBe true
  }

  it should "return false if baseStatus is OK and there is no TryMaxBlock in the stack" in {
    newSession.isFailed shouldBe false
  }

  it should "return false if baseStatus is OK and there is no failed TryMaxBlock in the stack" in {
    val session = newSession.copy(blockStack = List(TryMaxBlock("tryMax", nextAction)))

    session.isFailed shouldBe false
  }

  "status" should "be OK if the session is not failed" in {
    newSession.status shouldBe OK
  }

  it should "be KO if the session is failed" in {
    newSession.copy(baseStatus = KO).status shouldBe KO
  }
  "markAsSucceeded" should "only set the baseStatus to OK if it was not set and there is no TryMaxBlock in the stack" in {
    val session = newSession.copy(baseStatus = KO)
    val failedSession = session.markAsSucceeded

    failedSession should not be theSameInstanceAs(session)
    failedSession.baseStatus shouldBe OK
  }

  it should "leave the session unmodified if baseStatus is already OK and there is no TryMaxBlock in the stack" in {
    val session = newSession
    val failedSession = session.markAsSucceeded

    failedSession should be theSameInstanceAs session
  }

  it should "set the TryMaxBlock's status to KO if there is a TryMaxBlock in the stack, but leave the baseStatus unmodified" in {
    val session = newSession.copy(baseStatus = KO).enterGroup("root group").enterTryMax("tryMax", nextAction)
    val failedSession = session.markAsSucceeded

    failedSession.baseStatus shouldBe KO
    failedSession.blockStack.head.asInstanceOf[TryMaxBlock].status shouldBe OK
  }

  "markAsFailed" should "only set the baseStatus to KO if it was not set and there is no TryMaxBlock in the stack" in {
    val session = newSession
    val failedSession = session.markAsFailed

    failedSession should not be theSameInstanceAs(session)
    failedSession.baseStatus shouldBe KO
  }

  it should "leave the session unmodified if baseStatus is already KO and there is no TryMaxBlock in the stack" in {
    val session = newSession.copy(baseStatus = KO)
    val failedSession = session.markAsFailed

    failedSession should be theSameInstanceAs session
  }

  it should "set the TryMaxBlock's status to KO if there is a TryMaxBlock in the stack, but leave the baseStatus unmodified" in {
    val session = newSession.enterGroup("root group").enterTryMax("tryMax", nextAction)
    val failedSession = session.markAsFailed

    failedSession.baseStatus shouldBe OK
    failedSession.blockStack.head.asInstanceOf[TryMaxBlock].status shouldBe KO
  }

  "enterLoop" should "add an ExitASAPLoopBlock on top of the stack and init a counter when exitASAP = true" in {
    val session = newSession.enterLoop("loop", true.expressionSuccess, nextAction, exitASAP = true, timebased = false)

    session.blockStack.head shouldBe a[ExitAsapLoopBlock]
    session.contains("loop") shouldBe true
    session.attributes("loop") shouldBe 0
  }

  it should "add an ExitOnCompleteLoopBlock on top of the stack and init a counter when exitASAP = false" in {
    val session = newSession.enterLoop("loop", true.expressionSuccess, nextAction, exitASAP = false, timebased = false)

    session.blockStack.head shouldBe a[ExitOnCompleteLoopBlock]
    session.contains("loop") shouldBe true
    session.attributes("loop") shouldBe 0
  }

  "exitLoop" should "remove the LoopBlock from the top of the stack and its associated counter" in {
    val session = newSession.enterLoop("loop", true.expressionSuccess, nextAction, exitASAP = false, timebased = false)
    val sessionOutOfLoop = session.exitLoop

    sessionOutOfLoop.blockStack shouldBe empty
    sessionOutOfLoop.contains("loop") shouldBe false
  }

  it should "leave the stack unmodified if there's no LoopBlock on top of the stack" in {
    val session = newSession
    val unModifiedSession = session.exitLoop
    session should be theSameInstanceAs unModifiedSession
  }

  "initCounter" should "add a counter, initialized to 0, and a timestamp for the counter creation in the session" in {
    val session = newSession.initCounter("counter", withTimestamp = true)

    session.contains("counter") shouldBe true
    session.attributes("counter") shouldBe 0
    session.contains("timestamp.counter") shouldBe true
  }

  "incrementCounter" should "increment a counter in session" in {
    val session = newSession.initCounter("counter", withTimestamp = false)
    val sessionWithUpdatedCounter = session.incrementCounter("counter")

    sessionWithUpdatedCounter.attributes("counter") shouldBe 1
  }

  it should "should leave the session unmodified if there was no counter created with the specified name" in {
    val session = newSession
    val unModifiedSession = session.incrementCounter("counter")
    session should be theSameInstanceAs unModifiedSession
  }

  "removeCounter" should "remove a counter and its associated timestamp from the session" in {
    val session = newSession.initCounter("counter", withTimestamp = true)
    val sessionWithRemovedCounter = session.removeCounter("counter")

    sessionWithRemovedCounter.contains("counter") shouldBe false
    sessionWithRemovedCounter.contains("timestamp.counter") shouldBe false
  }

  it should "should leave the session unmodified if there was no counter created with the specified name" in {
    val session = newSession
    val unModifiedSession = session.removeCounter("counter")
    session should be theSameInstanceAs unModifiedSession
  }

  "update" should "apply sequentially all updates functions on the session" in {
    val session = newSession.update(List(_.set("foo", "bar"), _.set("quz", "qiz"), _.remove("foo")))

    session.contains("foo") shouldBe false
    session.contains("quz") shouldBe true
  }

  "terminate" should "call the userEnd function" in {
    var i = 0
    val session = newSession.copy(onExit = (s: Session) => i += 1)
    session.exit()

    i shouldBe 1
  }

  "MarkAsFailedUpdate function" should "mark as failed the session passed as parameter" in {
    val failedSession = Session.MarkAsFailedUpdate(newSession)
    failedSession.isFailed shouldBe true
  }

  "Identity function" should "return the same session instance" in {
    val session = newSession
    val unModifiedSession = Session.Identity(session)
    session should be theSameInstanceAs unModifiedSession
  }

  "as[T]" should "return the value when key is defined and value of the expected type" in {
    val session = newSession.set("foo", "bar")
    session("foo").as[String] shouldBe "bar"
  }

  it should "throw an exception when key isn't defined" in {
    val session = newSession
    a[java.util.NoSuchElementException] shouldBe thrownBy(session("foo").as[String])
  }

  it should "throw an exception when the value isn't of the expected type" in {
    val session = newSession.set("foo", "bar")
    a[java.lang.ClassCastException] shouldBe thrownBy(session("foo").as[Int])
  }

  "asOption[T]" should "return a Some(value) when key is defined and value of the expected type" in {
    val session = newSession.set("foo", "bar")
    session("foo").asOption[String] shouldBe Some("bar")
  }

  it should "return None when key isn't defined" in {
    val session = newSession
    session("foo").asOption[String] shouldBe None
  }

  it should "throw an exception when the value isn't of the expected type" in {
    val session = newSession.set("foo", "bar")
    a[ClassCastException] shouldBe thrownBy(session("foo").asOption[Int])
  }

  "validate[T]" should "return a Validation(value) when key is defined and value of the expected type" in {
    val session = newSession.set("foo", "bar")
    session("foo").validate[String] shouldBe Success("bar")
  }

  it should "return a Failure when key isn't defined" in {
    val session = newSession
    session("foo").validate[String] shouldBe a[Failure]
  }

  it should "return a Failure when the value isn't of the expected type" in {
    val session = newSession.set("foo", "bar")
    session("foo").validate[Int] shouldBe a[Failure]
  }
}

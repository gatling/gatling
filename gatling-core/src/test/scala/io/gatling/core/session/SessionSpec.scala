/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.session

import org.scalatest.{ FlatSpec, Matchers }

class SessionSpec extends FlatSpec with Matchers {

  def newSession = Session("scenario", "1")

  "setAll" should "set all give key/values pairs in session" in {
    val session = newSession.setAll("key" -> 1, "otherKey" -> 2)
    session.attributes.contains("key") shouldBe true
    session.attributes.contains("otherKey") shouldBe true
    session.attributes("key") shouldBe 1
    session.attributes("otherKey") shouldBe 2
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
    val timestamp = System.currentTimeMillis()
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

  "enterGroup" should "add a group block with its hierarchy is there are groups in the stack" in {
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
}

/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.jms.action

import org.scalatest.{ FlatSpec, Matchers }

import akka.testkit.TestActorRef
import io.gatling.core.CoreModule
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.message._
import io.gatling.core.result.writer.RequestEndMessage
import io.gatling.core.session.Session
import io.gatling.core.test.ActorSupport
import io.gatling.jms._
import io.gatling.jms.Predef._
import io.gatling.jms.check.JmsSimpleCheck

class JmsRequestTrackerActorWithMockWriter extends JmsRequestTrackerActor with MockDataWriterClient

class JmsRequestTrackerActorSpec extends FlatSpec with Matchers with MockMessage with CoreModule {

  implicit val configuration = GatlingConfiguration.loadForTest()

  def ignoreDrift(actual: Session) = {
    actual.drift shouldBe >(0L)
    actual.setDrift(0)
  }

  val session = Session("mockSession", "mockUserName")

  "JmsRequestTrackerActor" should "pass to next to next actor when matching message is received" in ActorSupport { testKit =>
    import testKit._

    val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

    tracker ! MessageSent("1", 15, 20, Nil, session, testActor, "success")
    tracker ! MessageReceived("1", 30, textMessage("test"))

    val nextSession = expectMsgType[Session]

    ignoreDrift(nextSession) shouldBe session
    val expected = RequestEndMessage("mockSession", "mockUserName", Nil, "success", RequestTimings(15, 20, 20, 30), OK, None, Nil)
    tracker.underlyingActor.dataWriterMsg should contain(expected)
  }

  it should "pass to next to next actor even if messages are out of sync" in ActorSupport { testKit =>
    import testKit._

    val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

    tracker ! MessageReceived("1", 30, textMessage("test"))
    tracker ! MessageSent("1", 15, 20, Nil, session, testActor, "outofsync")

    val nextSession = expectMsgType[Session]

    ignoreDrift(nextSession) shouldBe session
    val expected = RequestEndMessage("mockSession", "mockUserName", Nil, "outofsync", RequestTimings(15, 20, 20, 30), OK, None, Nil)
    tracker.underlyingActor.dataWriterMsg should contain(expected)
  }

  it should "pass KO to next actor when check fails" in ActorSupport { testKit =>
    import testKit._

    val failedCheck = JmsSimpleCheck(_ => false)
    val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

    tracker ! MessageSent("1", 15, 20, List(failedCheck), session, testActor, "failure")
    tracker ! MessageReceived("1", 30, textMessage("test"))

    val nextSession = expectMsgType[Session]

    ignoreDrift(nextSession) shouldBe session.markAsFailed
    val expected = RequestEndMessage("mockSession", "mockUserName", Nil, "failure", RequestTimings(15, 20, 20, 30), KO, Some("Jms check failed"), Nil)
    tracker.underlyingActor.dataWriterMsg should contain(expected)
  }

  it should "pass updated session to next actor if modified by checks" in ActorSupport { testKit =>
    import testKit._

    val check: JmsCheck = xpath("/id").saveAs("id")
    val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

    tracker ! MessageSent("1", 15, 20, List(check), session, testActor, "updated")
    tracker ! MessageReceived("1", 30, textMessage("<id>5</id>"))

    val nextSession = expectMsgType[Session]

    ignoreDrift(nextSession) shouldBe session.set("id", "5")
    val expected = RequestEndMessage("mockSession", "mockUserName", Nil, "updated", RequestTimings(15, 20, 20, 30), OK, None, Nil)
    tracker.underlyingActor.dataWriterMsg should contain(expected)
  }

  it should "pass information to session about response time in case group are used" in ActorSupport { testKit =>
    import testKit._

    val tracker = TestActorRef[JmsRequestTrackerActorWithMockWriter]

    val groupSession = session.enterGroup("group")
    tracker ! MessageSent("1", 15, 20, Nil, groupSession, testActor, "logGroupResponse")
    tracker ! MessageReceived("1", 30, textMessage("group"))

    val newSession = groupSession.logGroupRequest(15, OK)
    val nextSession1 = expectMsgType[Session]

    val failedCheck = JmsSimpleCheck(_ => false)
    tracker ! MessageSent("2", 25, 30, List(failedCheck), newSession, testActor, "logGroupResponse")
    tracker ! MessageReceived("2", 50, textMessage("group"))

    val nextSession2 = expectMsgType[Session]

    ignoreDrift(nextSession1) shouldBe newSession
    ignoreDrift(nextSession2) shouldBe newSession.logGroupRequest(25, KO).markAsFailed
  }
}

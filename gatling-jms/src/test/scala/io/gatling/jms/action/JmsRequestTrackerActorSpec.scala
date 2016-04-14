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
package io.gatling.jms.action

import io.gatling.AkkaSpec
import io.gatling.commons.stats.{ KO, OK }
import io.gatling.core.CoreDsl
import io.gatling.core.action.ActorDelegatingAction
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.stats.writer.ResponseMessage
import io.gatling.jms._
import io.gatling.jms.check.JmsSimpleCheck

import akka.testkit.TestActorRef

class JmsRequestTrackerActorSpec extends AkkaSpec with CoreDsl with JmsDsl with MockMessage {

  val configuration = GatlingConfiguration.loadForTest()

  def ignoreDrift(actual: Session) = {
    actual.drift shouldBe >(0L)
    actual.setDrift(0)
  }

  val session = Session("mockSession", 0)

  "JmsRequestTrackerActor" should "pass to next to next actor when matching message is received" in {
    val statsEngine = new MockStatsEngine
    val tracker = TestActorRef(new JmsRequestTrackerActor(statsEngine, configuration))

    tracker ! MessageSent("replyDestinationName", "1", 15, Nil, session, new ActorDelegatingAction("next", testActor), "success")
    tracker ! MessageReceived("replyDestinationName", "1", 30, textMessage("test"))

    val nextSession = expectMsgType[Session]

    ignoreDrift(nextSession) shouldBe session
    val expected = ResponseMessage("mockSession", 0, Nil, "success", ResponseTimings(15, 30), OK, None, None, Nil)
    statsEngine.dataWriterMsg should contain(expected)
  }

  it should "pass to next to next actor even if messages are out of sync" in {
    val statsEngine = new MockStatsEngine
    val tracker = TestActorRef(new JmsRequestTrackerActor(statsEngine, configuration))

    tracker ! MessageReceived("replyDestinationName", "1", 30, textMessage("test"))
    tracker ! MessageSent("replyDestinationName", "1", 15, Nil, session, new ActorDelegatingAction("next", testActor), "outofsync")

    val nextSession = expectMsgType[Session]

    ignoreDrift(nextSession) shouldBe session
    val expected = ResponseMessage("mockSession", 0, Nil, "outofsync", ResponseTimings(15, 30), OK, None, None, Nil)
    statsEngine.dataWriterMsg should contain(expected)
  }

  it should "pass KO to next actor when check fails" in {
    val failedCheck = JmsSimpleCheck(_ => false)
    val statsEngine = new MockStatsEngine
    val tracker = TestActorRef(new JmsRequestTrackerActor(statsEngine, configuration))

    tracker ! MessageSent("replyDestinationName", "1", 15, List(failedCheck), session, new ActorDelegatingAction("next", testActor), "failure")
    tracker ! MessageReceived("replyDestinationName", "1", 30, textMessage("test"))

    val nextSession = expectMsgType[Session]

    ignoreDrift(nextSession) shouldBe session.markAsFailed
    val expected = ResponseMessage("mockSession", 0, Nil, "failure", ResponseTimings(15, 30), KO, None, Some("Jms check failed"), Nil)
    statsEngine.dataWriterMsg should contain(expected)
  }

  it should "pass updated session to next actor if modified by checks" in {
    val check: JmsCheck = xpath("/id").saveAs("id")
    val statsEngine = new MockStatsEngine
    val tracker = TestActorRef(new JmsRequestTrackerActor(statsEngine, configuration))

    tracker ! MessageSent("replyDestinationName", "1", 15, List(check), session, new ActorDelegatingAction("next", testActor), "updated")
    tracker ! MessageReceived("replyDestinationName", "1", 30, textMessage("<id>5</id>"))

    val nextSession = expectMsgType[Session]

    ignoreDrift(nextSession) shouldBe session.set("id", "5")
    val expected = ResponseMessage("mockSession", 0, Nil, "updated", ResponseTimings(15, 30), OK, None, None, Nil)
    statsEngine.dataWriterMsg should contain(expected)
  }

  it should "pass information to session about response time in case group are used" in {
    val statsEngine = new MockStatsEngine
    val tracker = TestActorRef(new JmsRequestTrackerActor(statsEngine, configuration))

    val groupSession = session.enterGroup("group")
    tracker ! MessageSent("replyDestinationName", "1", 15, Nil, groupSession, new ActorDelegatingAction("next", testActor), "logGroupResponse")
    tracker ! MessageReceived("replyDestinationName", "1", 30, textMessage("group"))

    val newSession = groupSession.logGroupRequest(15, OK)
    val nextSession1 = expectMsgType[Session]

    val failedCheck = JmsSimpleCheck(_ => false)
    tracker ! MessageSent("replyDestinationName", "2", 25, List(failedCheck), newSession, new ActorDelegatingAction("next", testActor), "logGroupResponse")
    tracker ! MessageReceived("replyDestinationName", "2", 50, textMessage("group"))

    val nextSession2 = expectMsgType[Session]

    ignoreDrift(nextSession1) shouldBe newSession
    ignoreDrift(nextSession2) shouldBe newSession.logGroupRequest(25, KO).markAsFailed
  }
}

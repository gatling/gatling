/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.DefaultClock
import io.gatling.core.{ CoreDsl, EmptySession }
import io.gatling.core.action.ActorDelegatingAction
import io.gatling.core.actor.ActorSpec
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.jms._
import io.gatling.jms.client.JmsTracker

class JmsTrackerSpec extends ActorSpec with CoreDsl with JmsDsl with MockMessage with EmptySession {
  override val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private val clock = new DefaultClock

  "JmsRequestTrackerActor" should "pass to next to next actor when matching message is received" in {
    val statsEngine = new MockStatsEngine
    val nextActor = mockActorRef[Session]("next")
    val tracker = actorSystem.actorOf(JmsTracker.actor("jms-tracker", statsEngine, clock, configuration))

    tracker ! JmsTracker.Command.MessageSent("1", 15, 0, Nil, emptySession, new ActorDelegatingAction("next", nextActor), "success")
    tracker ! JmsTracker.Command.MessageReceived("1", 30, textMessage("test"))

    val nextSession = nextActor.expectMsgType[Session]()

    nextSession shouldBe emptySession
    val expected = MockStatsEngine.Message.Response(emptySession.scenario, Nil, "success", 15, 30, OK, None, None)
    statsEngine.messages should contain(expected)
  }

  it should "pass KO to next actor when check fails" in {
    val failedCheck = simpleCheck(_ => false)
    val statsEngine = new MockStatsEngine
    val nextActor = mockActorRef[Session]("next")
    val tracker = actorSystem.actorOf(JmsTracker.actor("jms-tracker", statsEngine, clock, configuration))

    tracker ! JmsTracker.Command.MessageSent("1", 15, 0, List(failedCheck), emptySession, new ActorDelegatingAction("next", nextActor), "failure")
    tracker ! JmsTracker.Command.MessageReceived("1", 30, textMessage("test"))

    val nextSession = nextActor.expectMsgType[Session]()

    nextSession shouldBe emptySession.markAsFailed
    val expected = MockStatsEngine.Message.Response(emptySession.scenario, Nil, "failure", 15, 30, KO, None, Some("JMS check failed"))
    statsEngine.messages should contain(expected)
  }

  it should "pass updated session to next actor if modified by checks" in {
    val check: JmsCheck = xpath("/id").saveAs("id")
    val statsEngine = new MockStatsEngine
    val nextActor = mockActorRef[Session]("next")
    val tracker = actorSystem.actorOf(JmsTracker.actor("jms-tracker", statsEngine, clock, configuration))

    tracker ! JmsTracker.Command.MessageSent("1", 15, 0, List(check), emptySession, new ActorDelegatingAction("next", nextActor), "updated")
    tracker ! JmsTracker.Command.MessageReceived("1", 30, textMessage("<id>5</id>"))

    val nextSession = nextActor.expectMsgType[Session]()

    nextSession shouldBe emptySession.set("id", "5")
    val expected = MockStatsEngine.Message.Response(emptySession.scenario, Nil, "updated", 15, 30, OK, None, None)
    statsEngine.messages should contain(expected)
  }

  it should "pass information to session about response time in case group are used" in {
    val statsEngine = new MockStatsEngine
    val nextActor = mockActorRef[Session]("next")
    val tracker = actorSystem.actorOf(JmsTracker.actor("jms-tracker", statsEngine, clock, configuration))

    val groupSession = emptySession.enterGroup("group", clock.nowMillis)
    tracker ! JmsTracker.Command.MessageSent("1", 15, 0, Nil, groupSession, new ActorDelegatingAction("next", nextActor), "logGroupResponse")
    tracker ! JmsTracker.Command.MessageReceived("1", 30, textMessage("group"))

    val newSession = groupSession.logGroupRequestTimings(15, 30)
    val nextSession1 = nextActor.expectMsgType[Session]()

    val failedCheck = simpleCheck(_ => false)
    tracker ! JmsTracker.Command.MessageSent("2", 25, 0, List(failedCheck), newSession, new ActorDelegatingAction("next", nextActor), "logGroupResponse")
    tracker ! JmsTracker.Command.MessageReceived("2", 50, textMessage("group"))

    val nextSession2 = nextActor.expectMsgType[Session]()

    nextSession1 shouldBe newSession
    nextSession2 shouldBe newSession.logGroupRequestTimings(25, 50).markAsFailed
  }
}

/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.core.action

import io.gatling.core.EmptySession
import io.gatling.core.actor._
import io.gatling.core.controller.Controller
import io.gatling.core.feeder.Feeder
import io.gatling.core.session._

class FeedActorSpec extends ActorSpec with EmptySession {
  private def createFeedActor[T](feeder: Feeder[T], controller: ActorRef[Controller.Command]): ActorRef[FeedMessage] =
    actorSystem.actorOf(FeedActor.actor(feeder, "feeder", None, generateJavaCollection = false, controller, feedCallSite = None))

  "FeedActor" should "force the simulation termination if the nb of records to pop is not strictly positive" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val feedActor = createFeedActor(Iterator.continually(Map("foo" -> "bar")), controller)
    val nextActor = mockActorRef[Session]("next")

    feedActor ! FeedMessage(emptySession, Some(0), new ActorDelegatingAction("next", nextActor))
    controller.expectMsgType[Controller.Command.Crash]()

    feedActor ! FeedMessage(emptySession, Some(-1), new ActorDelegatingAction("next", nextActor))
    controller.expectMsgType[Controller.Command.Crash]()
  }

  it should "force the simulation termination if the feeder is empty" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val feedActor = createFeedActor(Iterator.empty, controller)
    val nextActor = mockActorRef[Session]("next")

    feedActor ! FeedMessage(emptySession, None, new ActorDelegatingAction("next", nextActor))
    controller.expectMsgType[Controller.Command.Crash]()
  }

  it should "simply put an entry from the feeder in the session when polling 1 record at a time" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val feedActor = createFeedActor(Iterator.continually(Map("foo" -> "bar")), controller)
    val nextActor = mockActorRef[Session]("next")

    feedActor ! FeedMessage(emptySession, None, new ActorDelegatingAction("next", nextActor))

    val newSession = nextActor.expectMsgType[Session]()
    newSession.contains("foo") shouldBe true
    newSession("foo").as[String] shouldBe "bar"
  }

  it should "put entries from the feeder suffixed with an index in the session when polling multiple record at a time" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val feedActor = createFeedActor(Iterator.continually(Map("foo" -> "bar")), controller)
    val nextActor = mockActorRef[Session]("next")

    feedActor ! FeedMessage(emptySession, Some(2), new ActorDelegatingAction("next", nextActor))

    val newSession = nextActor.expectMsgType[Session]()
    newSession.contains("foo") shouldBe true
    newSession("foo").as[Seq[Any]] shouldBe Seq("bar", "bar")
  }
}

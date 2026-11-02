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

package io.gatling.core.action

import scala.concurrent.duration._

import io.gatling.core.EmptySession
import io.gatling.core.actor.ActorSpec
import io.gatling.core.session.Session

@SuppressWarnings(Array("org.wartremover.warts.ThreadSleep"))
class RendezVousSpec extends ActorSpec with EmptySession {

  "RendezVous" should "block the specified number of sessions until they have all reached it" in {
    val nextActor = mockActorRef[Session]("next")
    val rendezVous = RendezVous(3, None, actorSystem, new ActorDelegatingAction("next", nextActor))

    val session0 = emptySession.copy(userId = 0)
    val session1 = emptySession.copy(userId = 1)
    val session2 = emptySession.copy(userId = 2)
    val session3 = emptySession.copy(userId = 3)

    rendezVous ! session0
    Thread.sleep(100)
    nextActor.expectNoMsg()

    rendezVous ! session1
    Thread.sleep(100)
    nextActor.expectNoMsg()

    rendezVous ! session2
    Thread.sleep(100)
    nextActor.expectMsgType[Session]() shouldBe session0
    nextActor.expectMsgType[Session]() shouldBe session1
    nextActor.expectMsgType[Session]() shouldBe session2

  }

  it should "re-arm once pending users have been freed" in {
    val nextActor = mockActorRef[Session]("next")
    val rendezVous = RendezVous(2, None, actorSystem, new ActorDelegatingAction("next", nextActor))

    val session0 = emptySession.copy(userId = 0)
    val session1 = emptySession.copy(userId = 1)
    val session2 = emptySession.copy(userId = 2)
    val session3 = emptySession.copy(userId = 3)

    rendezVous ! session0
    rendezVous ! session1
    nextActor.expectMsgType[Session]() shouldBe session0
    nextActor.expectMsgType[Session]() shouldBe session1

    rendezVous ! session2
    Thread.sleep(100)
    nextActor.expectNoMsg()

    rendezVous ! session3
    nextActor.expectMsgType[Session]() shouldBe session2
    nextActor.expectMsgType[Session]() shouldBe session3
  }

  it should "release an incomplete batch when the timeout expires" in {
    val nextActor = mockActorRef[Session]("next")
    val rendezVous = RendezVous(3, Some(300.millis), actorSystem, new ActorDelegatingAction("next", nextActor))

    val session0 = emptySession.copy(userId = 0)
    val session1 = emptySession.copy(userId = 1)

    rendezVous ! session0
    rendezVous ! session1
    Thread.sleep(100)
    nextActor.expectNoMsg()

    nextActor.expectMsgType[Session]() shouldBe session0
    nextActor.expectMsgType[Session]() shouldBe session1
  }

  it should "start a new timeout for the next batch after a release" in {
    val nextActor = mockActorRef[Session]("next")
    val rendezVous = RendezVous(2, Some(300.millis), actorSystem, new ActorDelegatingAction("next", nextActor))

    val session0 = emptySession.copy(userId = 0)
    val session1 = emptySession.copy(userId = 1)
    val session2 = emptySession.copy(userId = 2)

    // complete batch: released immediately, its timeout must be discarded
    rendezVous ! session0
    rendezVous ! session1
    nextActor.expectMsgType[Session]() shouldBe session0
    nextActor.expectMsgType[Session]() shouldBe session1

    // the first batch's timeout must not release session2 early
    Thread.sleep(200)
    rendezVous ! session2
    Thread.sleep(200)
    nextActor.expectNoMsg()

    nextActor.expectMsgType[Session]() shouldBe session2
  }
}

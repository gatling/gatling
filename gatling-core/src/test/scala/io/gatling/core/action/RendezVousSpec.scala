/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util.DefaultClock
import io.gatling.core.actor.ActorSpec
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine

@SuppressWarnings(Array("org.wartremover.warts.ThreadSleep"))
class RendezVousSpec extends ActorSpec {
  private val clock = new DefaultClock

  "RendezVous" should "block the specified number of sessions until they have all reached it" in {
    val nextActor = mockActorRef[Session]("next")
    val rendezVous = RendezVous(3, actorSystem, mock[StatsEngine], clock, new ActorDelegatingAction("next", nextActor))

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

    rendezVous ! session3
    nextActor.expectMsgType[Session]() shouldBe session3
  }
}

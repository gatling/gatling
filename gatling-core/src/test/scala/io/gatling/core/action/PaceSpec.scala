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

import scala.concurrent.duration._

import io.gatling.commons.util.DefaultClock
import io.gatling.core.EmptySession
import io.gatling.core.Predef._
import io.gatling.core.actor.ActorSpec
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine

@SuppressWarnings(Array("org.wartremover.warts.ThreadSleep"))
class PaceSpec extends ActorSpec with EmptySession {
  private val clock = new DefaultClock

  private val interval = 3.seconds
  private val counterName = "paceCounter"

  "pace" should "run actions with a minimum wait time" in {
    val nextActor = mockActorRef[Session]("next")
    val pace = new Pace(interval, counterName, null, clock, new ActorDelegatingAction("next", nextActor))

    // Send session, expect response near-instantly
    pace ! emptySession
    val session1 = nextActor.expectMsgType[Session]()

    // Send second session, expect nothing for ~3 seconds, then a response
    pace ! session1

    Thread.sleep((interval - 1.second).toMillis)
    nextActor.expectNoMsg()

    val session2 = nextActor.expectMsgType[Session](2.seconds)

    // counter must have incremented by 3 seconds
    session2(counterName).as[Long] shouldBe session1(counterName).as[Long] + interval.toMillis +- 50
  }

  it should "run actions immediately if the minimum time has expired" in {
    val overrunTime = 1.second
    val nextActor = mockActorRef[Session]("next")
    val pace = new Pace(interval, counterName, null, clock, new ActorDelegatingAction("next", nextActor))

    // Send session, expect response near-instantly
    pace ! emptySession
    val session1 = nextActor.expectMsgType[Session]()

    // Wait 4 seconds - simulate overrunning action
    Thread.sleep((interval + overrunTime).toMillis)

    // Send second session, expect response near-instantly
    pace ! session1
    val session2 = nextActor.expectMsgType[Session](2.seconds)

    // counter must have incremented by 3 seconds
    session2(counterName).as[Long] shouldBe session1(counterName).as[Long] + overrunTime.toMillis + interval.toMillis +- 50
  }
}

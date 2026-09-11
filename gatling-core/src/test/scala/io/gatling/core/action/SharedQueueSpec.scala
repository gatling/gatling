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

import io.gatling.commons.util.DefaultClock
import io.gatling.commons.validation._
import io.gatling.core.EmptySession
import io.gatling.core.actor.{ ActorRef, ActorSpec, MockActorRef }
import io.gatling.core.session.Session
import io.gatling.core.stats.NoopStatsEngine

@SuppressWarnings(Array("org.wartremover.warts.ThreadSleep"))
class SharedQueueSpec extends ActorSpec with EmptySession {
  private val sweepPeriod = 50.milliseconds

  private def setUp(): (ActorRef[SharedQueueActor.Command], MockActorRef[Session], Action) = {
    val nextActor = mockActorRef[Session]("next")
    val next = new ActorDelegatingAction("next", nextActor)
    val queue = actorSystem.actorOf(new SharedQueueActor("test", new DefaultClock, sweepPeriod, "queue"))
    (queue, nextActor, next)
  }

  "SharedQueueActor" should "hand a value over to a virtual user taking after it was put" in {
    val (queue, nextActor, next) = setUp()

    queue ! SharedQueueActor.Command.Put(emptySession.copy(userId = 0), "value", next)
    nextActor.expectMsgType[Session]().userId shouldBe 0

    queue ! SharedQueueActor.Command.Take(emptySession.copy(userId = 1), "key", None, next)
    val taker = nextActor.expectMsgType[Session]()
    taker.userId shouldBe 1
    taker("key").as[String] shouldBe "value"
  }

  it should "park a virtual user taking from an empty queue until a value is put" in {
    val (queue, nextActor, next) = setUp()

    queue ! SharedQueueActor.Command.Take(emptySession.copy(userId = 0), "key", None, next)
    Thread.sleep(100)
    nextActor.expectNoMsg()

    queue ! SharedQueueActor.Command.Put(emptySession.copy(userId = 1), "value", next)

    // the parked taker is served first, then the putter moves on without waiting
    val taker = nextActor.expectMsgType[Session]()
    taker.userId shouldBe 0
    taker("key").as[String] shouldBe "value"
    nextActor.expectMsgType[Session]().userId shouldBe 1
  }

  it should "serve parked virtual users in the order they arrived" in {
    val (queue, nextActor, next) = setUp()

    queue ! SharedQueueActor.Command.Take(emptySession.copy(userId = 0), "key", None, next)
    queue ! SharedQueueActor.Command.Take(emptySession.copy(userId = 1), "key", None, next)
    Thread.sleep(100)
    nextActor.expectNoMsg()

    queue ! SharedQueueActor.Command.Put(emptySession.copy(userId = 2), "first", next)
    queue ! SharedQueueActor.Command.Put(emptySession.copy(userId = 3), "second", next)

    val firstTaker = nextActor.expectMsgType[Session]()
    firstTaker.userId shouldBe 0
    firstTaker("key").as[String] shouldBe "first"
    nextActor.expectMsgType[Session]().userId shouldBe 2

    val secondTaker = nextActor.expectMsgType[Session]()
    secondTaker.userId shouldBe 1
    secondTaker("key").as[String] shouldBe "second"
    nextActor.expectMsgType[Session]().userId shouldBe 3
  }

  it should "emit values in the order they were put" in {
    val (queue, nextActor, next) = setUp()

    queue ! SharedQueueActor.Command.Put(emptySession, "first", next)
    queue ! SharedQueueActor.Command.Put(emptySession, "second", next)
    nextActor.expectMsgType[Session]()
    nextActor.expectMsgType[Session]()

    queue ! SharedQueueActor.Command.Take(emptySession, "key", None, next)
    val firstTaker = nextActor.expectMsgType[Session]()
    firstTaker("key").as[String] shouldBe "first"

    queue ! SharedQueueActor.Command.Take(emptySession, "key", None, next)
    val secondTaker = nextActor.expectMsgType[Session]()
    secondTaker("key").as[String] shouldBe "second"
  }

  it should "fail a virtual user whose take times out" in {
    val (queue, nextActor, next) = setUp()

    queue ! SharedQueueActor.Command.Take(emptySession, "key", Some(100.milliseconds), next)
    nextActor.expectNoMsg()

    val timedOut = nextActor.expectMsgType[Session](2.seconds)
    timedOut.isFailed shouldBe true
    timedOut.contains("key") shouldBe false
  }

  it should "not fail a virtual user that was served before its take timed out" in {
    val (queue, nextActor, next) = setUp()

    queue ! SharedQueueActor.Command.Take(emptySession.copy(userId = 0), "key", Some(100.milliseconds), next)
    queue ! SharedQueueActor.Command.Put(emptySession.copy(userId = 1), "value", next)

    val taker = nextActor.expectMsgType[Session]()
    taker.userId shouldBe 0
    taker.isFailed shouldBe false
    taker("key").as[String] shouldBe "value"
    nextActor.expectMsgType[Session]().userId shouldBe 1

    // several sweeps must go by without failing the served virtual user
    Thread.sleep((sweepPeriod * 4).toMillis)
    nextActor.expectNoMsg()
  }

  it should "not let a served virtual user's former deadline fail its next take" in {
    val (queue, nextActor, next) = setUp()
    val session = emptySession.copy(userId = 0)

    // park with a timeout, get served, then park again with no timeout: the former deadline must be gone
    queue ! SharedQueueActor.Command.Take(session, "key", Some(100.milliseconds), next)
    queue ! SharedQueueActor.Command.Put(emptySession.copy(userId = 1), "value", next)
    nextActor.expectMsgType[Session]().userId shouldBe 0
    nextActor.expectMsgType[Session]().userId shouldBe 1

    queue ! SharedQueueActor.Command.Take(session, "key", None, next)
    Thread.sleep((sweepPeriod * 6).toMillis)
    nextActor.expectNoMsg()

    queue ! SharedQueueActor.Command.Put(emptySession.copy(userId = 2), "other", next)
    val taker = nextActor.expectMsgType[Session]()
    taker.userId shouldBe 0
    taker.isFailed shouldBe false
    taker("key").as[String] shouldBe "other"
  }

  it should "fail a poll on an empty queue instead of parking the virtual user" in {
    val (queue, nextActor, next) = setUp()

    queue ! SharedQueueActor.Command.Poll(emptySession, "key", next)
    val failed = nextActor.expectMsgType[Session]()
    failed.isFailed shouldBe true
    failed.contains("key") shouldBe false

    queue ! SharedQueueActor.Command.Put(emptySession, "value", next)
    nextActor.expectMsgType[Session]()

    queue ! SharedQueueActor.Command.Poll(emptySession, "key", next)
    val polled = nextActor.expectMsgType[Session]()
    polled.isFailed shouldBe false
    polled("key").as[String] shouldBe "value"
  }

  it should "fail a virtual user putting a null value instead of crashing" in {
    val nextActor = mockActorRef[Session]("next")
    val next = new ActorDelegatingAction("next", nextActor)
    val queue = actorSystem.actorOf(new SharedQueueActor("test", new DefaultClock, sweepPeriod, "queue"))
    val put = new SharedQueuePut(queue, "test", _ => (null: Any).success, NoopStatsEngine, new DefaultClock, next)

    put ! emptySession

    val failed = nextActor.expectMsgType[Session]()
    failed.isFailed shouldBe true

    // the queue must still be usable
    queue ! SharedQueueActor.Command.Size(emptySession, "size", next)
    val size = nextActor.expectMsgType[Session]()
    size("size").as[Int] shouldBe 0
  }

  it should "store the current size of the queue" in {
    val (queue, nextActor, next) = setUp()

    queue ! SharedQueueActor.Command.Size(emptySession, "size", next)
    val empty = nextActor.expectMsgType[Session]()
    empty("size").as[Int] shouldBe 0

    queue ! SharedQueueActor.Command.Put(emptySession, "value", next)
    nextActor.expectMsgType[Session]()

    queue ! SharedQueueActor.Command.Size(emptySession, "size", next)
    val nonEmpty = nextActor.expectMsgType[Session]()
    nonEmpty("size").as[Int] shouldBe 1
  }
}

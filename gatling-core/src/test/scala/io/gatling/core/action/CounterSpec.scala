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

import java.util.concurrent.atomic.AtomicInteger

import io.gatling.core.EmptySession
import io.gatling.core.actor.{ ActorRef, ActorSpec, MockActorRef }
import io.gatling.core.controller.Controller
import io.gatling.core.session._
import io.gatling.core.stats.NoopStatsEngine

class CounterSpec extends ActorSpec with EmptySession {
  private val Key = "counter"

  private def sharedCounter(
      start: Int,
      increment: Int,
      length: Int,
      wrapAround: Boolean,
      controller: ActorRef[Controller.Command],
      next: MockActorRef[Session]
  ): Counter =
    new Counter.Shared(Key, start, increment, length, wrapAround, new AtomicInteger, controller, null, new ActorDelegatingAction("next", next))

  private def perUserCounter(
      start: Int,
      increment: Int,
      length: Int,
      wrapAround: Boolean,
      controller: ActorRef[Controller.Command],
      next: MockActorRef[Session]
  ): Counter =
    new Counter.PerUser(Key, start, increment, length, wrapAround, controller, null, new ActorDelegatingAction("next", next))

  private def perUserDynamicCounter(
      start: Expression[Int],
      increment: Expression[Int],
      end: Expression[Int],
      wrapAround: Boolean,
      controller: ActorRef[Controller.Command],
      next: MockActorRef[Session]
  ): Counter =
    new Counter.PerUserDynamic(Key, start, increment, end, wrapAround, controller, NoopStatsEngine, new ActorDelegatingAction("next", next))

  private def expectValue(next: MockActorRef[Session]): Int = {
    val session = next.expectMsgType[Session]()
    session(Key).as[Int]
  }

  "valueCount" should "count the values in the range" in {
    Counter.valueCount(0, 1, 9) shouldBe 10L
    Counter.valueCount(1, 1, 10) shouldBe 10L
    Counter.valueCount(0, 10, 95) shouldBe 10L
    Counter.valueCount(0, 1, 0) shouldBe 1L
    Counter.valueCount(0, 1, Int.MaxValue) shouldBe 2147483648L
  }

  "SharedCounter" should "emit successive values, whatever the virtual user" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = sharedCounter(0, 1, 10, wrapAround = false, controller, next)

    counter ! emptySession
    counter ! emptySession
    counter ! emptySession

    expectValue(next) shouldBe 0
    expectValue(next) shouldBe 1
    expectValue(next) shouldBe 2
    controller.expectNoMsg()
  }

  it should "honor start and increment" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = sharedCounter(10, 5, 10, wrapAround = false, controller, next)

    counter ! emptySession
    counter ! emptySession

    expectValue(next) shouldBe 10
    expectValue(next) shouldBe 15
  }

  it should "stop the load generator once it has exhausted its range" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = sharedCounter(0, 1, 2, wrapAround = false, controller, next)

    counter ! emptySession
    counter ! emptySession
    expectValue(next) shouldBe 0
    expectValue(next) shouldBe 1
    controller.expectNoMsg()

    counter ! emptySession
    next.expectNoMsg()
    controller.expectMsgType[Controller.Command.StopLoadGenerator]()
  }

  it should "start over instead of stopping the load generator when wrapping around" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = sharedCounter(0, 1, 2, wrapAround = true, controller, next)

    counter ! emptySession
    counter ! emptySession
    counter ! emptySession
    counter ! emptySession

    expectValue(next) shouldBe 0
    expectValue(next) shouldBe 1
    expectValue(next) shouldBe 0
    expectValue(next) shouldBe 1
    controller.expectNoMsg()
  }

  "PerUserCounter" should "give each virtual user the very same sequence of values" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = perUserCounter(0, 1, 10, wrapAround = false, controller, next)

    counter ! emptySession
    val user1Pass1 = next.expectMsgType[Session]()
    user1Pass1(Key).as[Int] shouldBe 0

    counter ! user1Pass1
    val user1Pass2 = next.expectMsgType[Session]()
    user1Pass2(Key).as[Int] shouldBe 1

    // a brand new virtual user starts over
    counter ! emptySession
    expectValue(next) shouldBe 0
    controller.expectNoMsg()
  }

  it should "stop the load generator once a virtual user has exhausted its range" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = perUserCounter(0, 1, 1, wrapAround = false, controller, next)

    counter ! emptySession
    val pass1 = next.expectMsgType[Session]()
    pass1(Key).as[Int] shouldBe 0

    counter ! pass1
    next.expectNoMsg()
    controller.expectMsgType[Controller.Command.StopLoadGenerator]()
  }

  "PerUserDynamicCounter" should "resolve its range from each virtual user's Session" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = perUserDynamicCounter(_("start").validate[Int], _("increment").validate[Int], _("end").validate[Int], wrapAround = false, controller, next)

    val user1 = emptySession.set("start", 10).set("increment", 5).set("end", 100)
    val user2 = emptySession.set("start", 0).set("increment", 1).set("end", 100)

    counter ! user1
    val user1Pass1 = next.expectMsgType[Session]()
    user1Pass1(Key).as[Int] shouldBe 10

    counter ! user1Pass1
    expectValue(next) shouldBe 15

    counter ! user2
    expectValue(next) shouldBe 0
    controller.expectNoMsg()
  }

  it should "resolve its range once per virtual user, on its first pass" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = perUserDynamicCounter(_("start").validate[Int], _("increment").validate[Int], _("end").validate[Int], wrapAround = false, controller, next)

    counter ! emptySession.set("start", 10).set("increment", 5).set("end", 100)
    val pass1 = next.expectMsgType[Session]()
    pass1(Key).as[Int] shouldBe 10

    // the values the range was computed from are gone, the sequence goes on all the same
    counter ! pass1.remove("start").remove("increment").remove("end")
    val pass2 = next.expectMsgType[Session]()
    pass2(Key).as[Int] shouldBe 15

    // and they can't shift it either
    counter ! pass2.set("start", 1000).set("increment", 1).set("end", 2000)
    expectValue(next) shouldBe 20
    controller.expectNoMsg()
  }

  it should "stop the load generator once a virtual user has exhausted its own range" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = perUserDynamicCounter(0.expressionSuccess, 1.expressionSuccess, _("end").validate[Int], wrapAround = false, controller, next)

    val session = emptySession.set("end", 0)

    counter ! session
    val pass1 = next.expectMsgType[Session]()
    pass1(Key).as[Int] shouldBe 0

    counter ! pass1
    next.expectNoMsg()
    controller.expectMsgType[Controller.Command.StopLoadGenerator]()
  }

  it should "start over instead of stopping the load generator when wrapping around" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = perUserDynamicCounter(0.expressionSuccess, 1.expressionSuccess, _("end").validate[Int], wrapAround = true, controller, next)

    val session = emptySession.set("end", 1)

    counter ! session
    val pass1 = next.expectMsgType[Session]()
    pass1(Key).as[Int] shouldBe 0

    counter ! pass1
    val pass2 = next.expectMsgType[Session]()
    pass2(Key).as[Int] shouldBe 1

    counter ! pass2
    expectValue(next) shouldBe 0
    controller.expectNoMsg()
  }

  it should "fail the virtual user when its range can't be resolved" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = perUserDynamicCounter(_("start").validate[Int], 1.expressionSuccess, 100.expressionSuccess, wrapAround = false, controller, next)

    counter ! emptySession

    next.expectMsgType[Session]().isFailed shouldBe true
    controller.expectNoMsg()
  }

  it should "fail the virtual user when its range is invalid" in {
    val controller = mockActorRef[Controller.Command]("controller")
    val next = mockActorRef[Session]("next")
    val counter = perUserDynamicCounter(_("start").validate[Int], 1.expressionSuccess, 100.expressionSuccess, wrapAround = false, controller, next)

    counter ! emptySession.set("start", -1)

    next.expectMsgType[Session]().isFailed shouldBe true
    controller.expectNoMsg()
  }
}

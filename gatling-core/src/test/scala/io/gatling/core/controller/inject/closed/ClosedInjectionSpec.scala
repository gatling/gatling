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

package io.gatling.core.controller.inject.closed

import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

import io.gatling.commons.util.Clock
import io.gatling.core.scenario.Scenario

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

object ClosedInjectionSpec {
  private final class ManualClock extends Clock {
    private var now = 0L
    def set(time: FiniteDuration): Unit = now = time.toMillis
    override def nowMillis: Long = now
  }

  private final class TestClosedInjection(steps: List[ClosedInjectionStep], clock: ManualClock)
      extends ClosedInjection(
        steps,
        steps.foldLeft(Duration.Zero)((acc, step) => acc.plus(step.duration)),
        steps.forall(_.isEmpty),
        new Scenario("foo", null, identity, _ => (), null),
        new AtomicLong,
        0L,
        null,
        null,
        clock
      ) {
    val delays: ArrayBuffer[FiniteDuration] = ArrayBuffer.empty

    override protected def scheduleUser(delay: FiniteDuration): Unit = delays += delay
  }
}

class ClosedInjectionSpec extends AnyFlatSpecLike with Matchers {
  import ClosedInjectionSpec._

  private val clock = new ManualClock

  private def injectBatchAt(injection: ClosedInjection, time: FiniteDuration): Unit = {
    clock.set(time)
    injection.injectBatch(1.second)
  }

  private def endUserAt(injection: ClosedInjection, time: FiniteDuration): Unit = {
    clock.set(time)
    injection.endUser()
  }

  "ClosedInjection" should "target the concurrent users based on elapsed time, not on the number of batches" in {
    val injection = new TestClosedInjection(List(RampConcurrentUsersInjection(0, 10, 10.seconds)), clock)

    injectBatchAt(injection, 400.millis) // targets the value at 1.4s
    injection.delays.size shouldBe 1

    injectBatchAt(injection, 5400.millis) // targets the value at 6.4s, even though this is only the 2nd batch
    injection.delays.size shouldBe 6
    injection.isAllUsersScheduled shouldBe false
  }

  it should "not inject more users while the target is reached" in {
    val injection = new TestClosedInjection(List(ConstantConcurrentUsersInjection(2, 3.seconds)), clock)

    injectBatchAt(injection, 400.millis)
    injection.delays.size shouldBe 2
    all(injection.delays) should be < 1.second

    injectBatchAt(injection, 1400.millis)
    injection.delays.size shouldBe 2
    injection.isAllUsersScheduled shouldBe false
  }

  it should "mark all users as scheduled on the last batch and spread users over the remaining time only" in {
    val injection = new TestClosedInjection(List(ConstantConcurrentUsersInjection(10, 3.seconds)), clock)

    injectBatchAt(injection, 2500.millis)
    injection.delays.size shouldBe 10
    all(injection.delays) should be < 500.millis
    injection.isAllUsersScheduled shouldBe true
  }

  it should "replace stopped users until the end of the injection, then complete once all users have stopped" in {
    val injection = new TestClosedInjection(List(ConstantConcurrentUsersInjection(2, 3.seconds)), clock)

    injectBatchAt(injection, 400.millis)
    injectBatchAt(injection, 1400.millis)
    injectBatchAt(injection, 2400.millis)
    injection.isAllUsersScheduled shouldBe true
    injection.delays.size shouldBe 2

    endUserAt(injection, 2900.millis)
    injection.delays.size shouldBe 3 // replaced
    injection.delays.last shouldBe Duration.Zero

    endUserAt(injection, 3100.millis)
    injection.delays.size shouldBe 3 // not replaced, injection is over
    injection.isAllUsersStopped shouldBe false

    endUserAt(injection, 3200.millis)
    injection.isAllUsersStopped shouldBe true
  }

  it should "not inject any user when the first batch happens after the end of the injection" in {
    val injection = new TestClosedInjection(List(ConstantConcurrentUsersInjection(2, 3.seconds)), clock)

    injectBatchAt(injection, 3500.millis)
    injection.delays shouldBe empty
    injection.isAllUsersScheduled shouldBe true
    injection.isAllUsersStopped shouldBe true
  }
}

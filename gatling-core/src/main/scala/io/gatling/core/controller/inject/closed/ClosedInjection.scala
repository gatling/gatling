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

import scala.concurrent.duration._

import io.gatling.commons.util.Clock
import io.gatling.core.controller.inject.Injection
import io.gatling.core.scenario.Scenario
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.Shard

import io.netty.channel.EventLoopGroup

private[inject] class ClosedInjection(
    steps: Iterable[ClosedInjectionStep],
    override val duration: FiniteDuration,
    override val isEmpty: Boolean,
    scenario: Scenario,
    userIdGen: AtomicLong,
    startTime: Long,
    eventLoopGroup: EventLoopGroup,
    statsEngine: StatsEngine,
    clock: Clock
) extends Injection(scenario, userIdGen, eventLoopGroup, statsEngine) {
  private val offsetedSteps: Array[(FiniteDuration, ClosedInjectionStep)] = {
    var offset: FiniteDuration = Duration.Zero

    steps.map { step =>
      offset = offset + step.duration
      offset -> step
    }.toArray
  }

  private var _thisBatchTarget = 0

  private def elapsed: FiniteDuration = (clock.nowMillis - startTime).millis

  override def injectBatch(batchWindow: FiniteDuration): Unit =
    if (!isAllUsersScheduled) {
      // based on elapsed time rather than on the number of batches, as the injection might start in between injector ticks
      val batchStart = elapsed

      if (batchStart >= duration) {
        _thisBatchTarget = 0
        setAllScheduled()
      } else {
        // target the concurrent users at the end of this batch window, without going past the end of the injection
        val batchEnd = (batchStart + batchWindow).min(duration)
        _thisBatchTarget = offsetedSteps.find { case (off, _) => batchEnd <= off }.fold(0) { case (off, step) => step.valueAt(batchEnd - off + step.duration) }
        val missingUsers = _thisBatchTarget - getConcurrentUsers

        if (missingUsers > 0) {
          for {
            (number, millis) <- Shard.shards(missingUsers, (batchEnd - batchStart).toMillis.toInt.max(1)).zipWithIndex
            if number > 0
          } (0 until number.toInt).foreach(_ => injectUser(millis.milliseconds))
        }

        if (batchEnd == duration) {
          // last batch: no more batches, only replacements in endUser until the end of the injection
          // so the scenario can complete (and its children start) as soon as its last users stop
          setAllScheduled()
        }
      }
    }

  override def endUser(): Unit = {
    incrementStoppedUsers()
    if (getConcurrentUsers < _thisBatchTarget && elapsed < duration) {
      // start a new user
      injectUser(Duration.Zero)
    }
  }
}

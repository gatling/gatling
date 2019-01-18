/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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
import io.gatling.core.controller.inject.Workload
import io.gatling.core.scenario.Scenario
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.writer.UserMessage
import io.gatling.core.util.Shard

import akka.actor.ActorSystem

class ClosedWorkload(scenario: Scenario, steps: Iterable[ClosedInjectionStep], userIdGen: AtomicLong, startTime: Long, system: ActorSystem, statsEngine: StatsEngine, clock: Clock)
  extends Workload(scenario, userIdGen, startTime, system, statsEngine, clock) {

  private val offsetedSteps: Array[(FiniteDuration, ClosedInjectionStep)] = {
    var offset: FiniteDuration = Duration.Zero

    steps.map { step =>
      offset = offset + step.duration
      offset -> step
    }.toArray
  }

  private var offset: FiniteDuration = Duration.Zero
  private var _thisBatchTarget = 0
  private var _thisBatchStarted = 0

  override def injectBatch(batchWindow: FiniteDuration): Unit = {
    _thisBatchTarget = 0
    _thisBatchStarted = 0
    offset = offset + batchWindow
    val currentStep: Option[(FiniteDuration, ClosedInjectionStep)] = offsetedSteps.find { case (off, _) => offset <= off }

    currentStep match {
      case Some((off, step)) =>
        _thisBatchTarget = step.valueAt(offset - off + step.duration)
        val missingUsers = _thisBatchTarget - getConcurrentUsers

        if (missingUsers > 0) {
          for {
            (number, millis) <- Shard.shards(missingUsers, batchWindow.toMillis.toInt).zipWithIndex
            if number > 0
          } (0 until number.toInt).foreach(_ => injectUser(millis milliseconds))
        }

      case _ => setAllScheduled()
    }
  }

  override def endUser(userMessage: UserMessage): Unit = {
    statsEngine.logUser(userMessage)
    incrementStoppedUsers()
    if (getConcurrentUsers < _thisBatchTarget && !isAllUsersScheduled) {
      // start a new user
      injectUser(Duration.Zero)
    }
  }
}

/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.core.controller.inject.open

import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.util.Clock
import io.gatling.core.controller.inject.Workload
import io.gatling.core.scenario.Scenario
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.writer.UserMessage

import akka.actor.ActorSystem

class OpenWorkload(scenario: Scenario, stream: UserStream, userIdGen: AtomicLong, startTime: Long, system: ActorSystem, statsEngine: StatsEngine, clock: Clock)
  extends Workload(scenario, userIdGen, startTime, system, statsEngine, clock) {

  override def injectBatch(batchWindow: FiniteDuration): Unit = {
    val result = stream.withStream(batchWindow, clock.nowMillis, startTime)(injectUser)
    logger.debug(s"Injecting ${result.count} users in scenario ${scenario.name}, continue=${result.continue}")
    if (!result.continue) {
      setAllScheduled()
    }
  }

  override def endUser(userMessage: UserMessage): Unit = {
    statsEngine.logUser(userMessage)
    incrementStoppedUsers()
  }
}

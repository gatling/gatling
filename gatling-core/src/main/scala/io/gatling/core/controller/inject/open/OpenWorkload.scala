/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
import io.gatling.core.stats.writer.UserEndMessage

import io.netty.channel.EventLoopGroup

class OpenWorkload(
    stream: UserStream,
    override val duration: FiniteDuration,
    override val isEmpty: Boolean,
    scenario: Scenario,
    userIdGen: AtomicLong,
    startTime: Long,
    eventLoopGroup: EventLoopGroup,
    statsEngine: StatsEngine,
    clock: Clock
) extends Workload(scenario, userIdGen, eventLoopGroup, statsEngine, clock) {

  override def injectBatch(batchWindow: FiniteDuration): Unit = {
    val result = stream.withStream(batchWindow, clock.nowMillis, startTime)(injectUser)
    if (!isEmpty) {
      logger.debug(s"Injecting ${result.count} users in scenario ${scenario.name}, continue=${result.continue}")
    }
    if (!result.continue) {
      setAllScheduled()
    }
  }

  override def endUser(userMessage: UserEndMessage): Unit = {
    statsEngine.logUserEnd(userMessage)
    incrementStoppedUsers()
  }
}

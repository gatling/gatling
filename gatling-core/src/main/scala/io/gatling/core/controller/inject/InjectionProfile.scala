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

package io.gatling.core.controller.inject

import java.util.concurrent.atomic.AtomicLong

import io.gatling.commons.util.Clock
import io.gatling.core.scenario.Scenario
import io.gatling.core.stats.StatsEngine

import io.netty.channel.EventLoopGroup

trait InjectionProfileFactory[-InjectionStep] {

  def profile(steps: Iterable[InjectionStep]): InjectionProfile
}

trait InjectionProfile {

  def totalUserCount: Option[Long]

  def workload(
      scenario: Scenario,
      userIdGen: AtomicLong,
      startTime: Long,
      eventLoopGroup: EventLoopGroup,
      statsEngine: StatsEngine,
      clock: Clock
  ): Workload
}

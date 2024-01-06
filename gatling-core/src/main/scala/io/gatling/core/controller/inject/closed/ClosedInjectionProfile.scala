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

package io.gatling.core.controller.inject.closed

import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.duration.Duration

import io.gatling.commons.util.Clock
import io.gatling.core.controller.inject.{ InjectionProfile, Workload }
import io.gatling.core.scenario.Scenario
import io.gatling.core.stats.StatsEngine

import io.netty.channel.EventLoopGroup

private[core] final class ClosedInjectionProfile(steps: List[ClosedInjectionStep]) extends InjectionProfile {
  // doesn't make sense for ClosedInjectionProfile
  override def totalUserCount: Option[Long] = None

  override private[inject] def workload(
      scenario: Scenario,
      userIdGen: AtomicLong,
      startTime: Long,
      eventLoopGroup: EventLoopGroup,
      statsEngine: StatsEngine,
      clock: Clock
  ): Workload =
    new ClosedWorkload(
      steps,
      steps.foldLeft(Duration.Zero)((acc, step) => acc.plus(step.duration)),
      steps.forall(_.isEmpty),
      scenario,
      userIdGen,
      eventLoopGroup,
      statsEngine
    )

  override def toString = s"ClosedInjectionProfile($steps)"

  // [e]
  //
  //
  // [e]
}

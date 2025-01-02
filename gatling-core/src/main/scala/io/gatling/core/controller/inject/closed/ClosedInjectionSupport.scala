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

package io.gatling.core.controller.inject.closed

import scala.concurrent.duration._

import io.gatling.core.controller.inject.InjectionProfileFactory

object ClosedInjectionBuilder {
  def newEachLevelLasting(usersIncrement: Int, nbOfSteps: Int): Stairs.EachLevelLasting =
    Stairs.EachLevelLasting(usersIncrement, nbOfSteps)

  final case class Constant(number: Int) {
    def during(d: FiniteDuration): ClosedInjectionStep = ConstantConcurrentUsersInjection(number, d)
  }

  object Ramp {
    final case class To(from: Int) {
      def to(t: Int): During = During(from, t)
    }

    final case class During(from: Int, to: Int) {
      def during(d: FiniteDuration): ClosedInjectionStep = RampConcurrentUsersInjection(from, to, d)
    }
  }

  object Stairs {
    final case class Times(concurrentUsers: Int) {
      def times(levels: Int): EachLevelLasting = EachLevelLasting(concurrentUsers, levels)
    }

    final case class EachLevelLasting(usersIncrement: Int, levels: Int) {
      def eachLevelLasting(levelDuration: FiniteDuration): StairsConcurrentUsersCompositeStep =
        StairsConcurrentUsersCompositeStep(usersIncrement, levels, levelDuration, 0, Duration.Zero)
    }
  }
}

object ClosedInjectionSupport {
  val ClosedInjectionProfileFactory: InjectionProfileFactory[ClosedInjectionStep] =
    (steps: Iterable[ClosedInjectionStep]) => new ClosedInjectionProfile(steps.toList)
}

trait ClosedInjectionSupport {
  implicit def closedInjectionProfileFactory: InjectionProfileFactory[ClosedInjectionStep] =
    ClosedInjectionSupport.ClosedInjectionProfileFactory

  def constantConcurrentUsers(number: Int): ClosedInjectionBuilder.Constant = ClosedInjectionBuilder.Constant(number)

  def rampConcurrentUsers(from: Int): ClosedInjectionBuilder.Ramp.To = ClosedInjectionBuilder.Ramp.To(from)

  def incrementConcurrentUsers(concurrentUsers: Int): ClosedInjectionBuilder.Stairs.Times = ClosedInjectionBuilder.Stairs.Times(concurrentUsers)
}

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

package io.gatling.core.controller.inject.open

import scala.concurrent.duration._

import io.gatling.core.controller.inject.InjectionProfileFactory

object OpenInjectionBuilder {
  final case class Ramp(users: Int) {
    def during(d: FiniteDuration): OpenInjectionStep = RampOpenInjection(users, d)
  }

  final case class StressPeak(users: Int) {
    def during(d: FiniteDuration): OpenInjectionStep = HeavisideOpenInjection(users, d)
  }
  final case class ConstantRate(rate: Double) {
    def during(d: FiniteDuration): ConstantRateOpenInjection = ConstantRateOpenInjection(rate, d)
  }

  object RampRate {
    final case class To(rate1: Double) {
      def to(rate2: Double): During = During(rate1, rate2)
    }
    final case class During(rate1: Double, rate2: Double) {
      def during(d: FiniteDuration): RampRateOpenInjection = RampRateOpenInjection(rate1, rate2, d)
    }
  }

  object Stairs {
    final case class Times(rateIncrement: Double) {
      def times(levels: Int): EachLevelLasting = EachLevelLasting(rateIncrement, levels)
    }

    final case class EachLevelLasting(rateIncrement: Double, levels: Int) {
      def eachLevelLasting(d: FiniteDuration): StairsUsersPerSecCompositeStep =
        StairsUsersPerSecCompositeStep(rateIncrement, levels, d, 0, Duration.Zero)
    }
  }
}

object OpenInjectionSupport {
  val OpenInjectionProfileFactory: InjectionProfileFactory[OpenInjectionStep] =
    (steps: Iterable[OpenInjectionStep]) => new OpenInjectionProfile(steps.toList)
}

trait OpenInjectionSupport {
  implicit def openInjectionProfileFactory: InjectionProfileFactory[OpenInjectionStep] =
    OpenInjectionSupport.OpenInjectionProfileFactory

  def rampUsers(users: Int): OpenInjectionBuilder.Ramp = OpenInjectionBuilder.Ramp(users)
  def stressPeakUsers(users: Int): OpenInjectionBuilder.StressPeak = OpenInjectionBuilder.StressPeak(users)
  def atOnceUsers(users: Int): OpenInjectionStep = AtOnceOpenInjection(users)

  def constantUsersPerSec(rate: Double): OpenInjectionBuilder.ConstantRate = OpenInjectionBuilder.ConstantRate(rate)
  def rampUsersPerSec(rate1: Double): OpenInjectionBuilder.RampRate.To = OpenInjectionBuilder.RampRate.To(rate1)

  def nothingFor(d: FiniteDuration): OpenInjectionStep = NothingForOpenInjection(d)

  def incrementUsersPerSec(usersPerSec: Double): OpenInjectionBuilder.Stairs.Times = OpenInjectionBuilder.Stairs.Times(usersPerSec)
}

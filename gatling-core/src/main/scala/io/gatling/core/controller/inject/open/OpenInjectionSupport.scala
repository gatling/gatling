/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

import io.gatling.core.controller.inject.{ InjectionProfile, InjectionProfileFactory, MetaInjectionProfile }

object OpenInjectionSupport {

  val OpenInjectionProfileFactory: InjectionProfileFactory[OpenInjectionStep] =
    (steps: Iterable[OpenInjectionStep]) => OpenInjectionProfile(steps)
}

trait OpenInjectionSupport extends MetaOpenInjectionSupport {

  implicit def openInjectionProfileFactory: InjectionProfileFactory[OpenInjectionStep] =
    OpenInjectionSupport.OpenInjectionProfileFactory

  case class RampBuilder(users: Int) {
    def during(d: FiniteDuration) = RampOpenInjection(users, d)
  }
  case class HeavisideBuilder(users: Int) {
    def during(d: FiniteDuration) = HeavisideOpenInjection(users, d)
  }
  case class ConstantRateBuilder(rate: Double) {
    def during(d: FiniteDuration) = ConstantRateOpenInjection(rate, d)
  }
  case class PartialRampRateBuilder(rate1: Double) {
    def to(rate2: Double) = RampRateBuilder(rate1, rate2)
  }
  case class RampRateBuilder(rate1: Double, rate2: Double) {
    def during(d: FiniteDuration) = RampRateOpenInjection(rate1, rate2, d)
  }

  def rampUsers(users: Int) = RampBuilder(users)
  def heavisideUsers(users: Int) = HeavisideBuilder(users)
  def atOnceUsers(users: Int) = AtOnceOpenInjection(users)

  def constantUsersPerSec(rate: Double) = ConstantRateBuilder(rate)
  def rampUsersPerSec(rate1: Double) = PartialRampRateBuilder(rate1)

  def nothingFor(d: FiniteDuration) = NothingForOpenInjection(d)
}

trait MetaOpenInjectionSupport {

  case class IncrementTest(
      usersPerSec:   Double,
      nbOfSteps:     Int,
      duration:      FiniteDuration,
      startingUsers: Double,
      rampDuration:  FiniteDuration
  ) extends MetaInjectionProfile {
    def startingFrom(startingUsers: Int): IncrementTest = this.copy(startingUsers = startingUsers)
    def separatedByRampsLasting(duration: FiniteDuration): IncrementTest = this.copy(rampDuration = duration)

    private[inject] def getInjectionSteps: Iterable[OpenInjectionStep] =
      (1 to nbOfSteps).foldLeft(Iterable.empty[OpenInjectionStep]) { (acc, currentStep) =>
        val step = if (startingUsers > 0) currentStep - 1 else currentStep
        val newRate = startingUsers + step * usersPerSec

        val newInjectionsSteps = if (currentStep < nbOfSteps && rampDuration > Duration.Zero) {
          Seq(ConstantRateOpenInjection(newRate, duration), RampRateOpenInjection(newRate, newRate + usersPerSec, rampDuration))
        } else {
          Seq(ConstantRateOpenInjection(newRate, duration))
        }
        acc ++ newInjectionsSteps
      }

    def profile: InjectionProfile = OpenInjectionProfile(getInjectionSteps)
  }

  case class IncrementTestBuilderWithTime(usersPerSec: Double, nbOfSteps: Int) {
    def eachLevelLasting(d: FiniteDuration) = IncrementTest(usersPerSec, nbOfSteps, d, 0, Duration.Zero)
  }

  case class IncrementTestBuilder(usersPerSec: Double) {
    def times(nbOfSteps: Int) = IncrementTestBuilderWithTime(usersPerSec, nbOfSteps)
  }

  def incrementUsersPerSec(usersPerSec: Double) = IncrementTestBuilder(usersPerSec)
}

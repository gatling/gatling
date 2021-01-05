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

import scala.concurrent.duration._

import io.gatling.core.controller.inject.InjectionProfileFactory

final case class RampBuilder(users: Int) {
  def during(d: FiniteDuration): OpenInjectionStep = RampOpenInjection(users, d)
}
final case class HeavisideBuilder(users: Int) {
  def during(d: FiniteDuration): OpenInjectionStep = HeavisideOpenInjection(users, d)
}
final case class ConstantRateBuilder(rate: Double) {
  def during(d: FiniteDuration): ConstantRateOpenInjection = ConstantRateOpenInjection(rate, d)
}
final case class PartialRampRateBuilder(rate1: Double) {
  def to(rate2: Double): RampRateBuilder = RampRateBuilder(rate1, rate2)
}
final case class RampRateBuilder(rate1: Double, rate2: Double) {
  def during(d: FiniteDuration): RampRateOpenInjection = RampRateOpenInjection(rate1, rate2, d)
}

final case class IncreasingUsersPerSecProfileBuilderWithTime(usersPerSec: Double, nbOfSteps: Int) {
  def eachLevelLasting(d: FiniteDuration): IncreasingUsersPerSecCompositeStep = IncreasingUsersPerSecCompositeStep(usersPerSec, nbOfSteps, d, 0, Duration.Zero)
}

final case class IncreasingUsersPerSecProfileBuilder(usersPerSec: Double) {
  def times(nbOfSteps: Int): IncreasingUsersPerSecProfileBuilderWithTime = IncreasingUsersPerSecProfileBuilderWithTime(usersPerSec, nbOfSteps)
}

object OpenInjectionSupport {

  val OpenInjectionProfileFactory: InjectionProfileFactory[OpenInjectionStep] =
    (steps: Iterable[OpenInjectionStep]) => new OpenInjectionProfile(steps)
}

trait OpenInjectionSupport {

  implicit def openInjectionProfileFactory: InjectionProfileFactory[OpenInjectionStep] =
    OpenInjectionSupport.OpenInjectionProfileFactory

  def rampUsers(users: Int): RampBuilder = RampBuilder(users)
  def heavisideUsers(users: Int): HeavisideBuilder = HeavisideBuilder(users)
  def atOnceUsers(users: Int): OpenInjectionStep = AtOnceOpenInjection(users)

  def constantUsersPerSec(rate: Double): ConstantRateBuilder = ConstantRateBuilder(rate)
  def rampUsersPerSec(rate1: Double): PartialRampRateBuilder = PartialRampRateBuilder(rate1)

  def nothingFor(d: FiniteDuration): OpenInjectionStep = NothingForOpenInjection(d)

  def incrementUsersPerSec(usersPerSec: Double): IncreasingUsersPerSecProfileBuilder = IncreasingUsersPerSecProfileBuilder(usersPerSec)
}

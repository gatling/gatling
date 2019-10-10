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

package io.gatling.core.controller.inject.open

import scala.concurrent.duration._

import io.gatling.core.controller.inject.InjectionProfileFactory

final case class RampBuilder(users: Int) {
  def during(d: FiniteDuration) = RampOpenInjection(users, d)
}
final case class HeavisideBuilder(users: Int) {
  def during(d: FiniteDuration) = HeavisideOpenInjection(users, d)
}
final case class ConstantRateBuilder(rate: Double) {
  def during(d: FiniteDuration) = ConstantRateOpenInjection(rate, d)
}
final case class PartialRampRateBuilder(rate1: Double) {
  def to(rate2: Double) = RampRateBuilder(rate1, rate2)
}
final case class RampRateBuilder(rate1: Double, rate2: Double) {
  def during(d: FiniteDuration) = RampRateOpenInjection(rate1, rate2, d)
}

final case class IncreasingUsersPerSecProfileBuilderWithTime(usersPerSec: Double, nbOfSteps: Int) {
  def eachLevelLasting(d: FiniteDuration) = IncreasingUsersPerSecCompositeStep(usersPerSec, nbOfSteps, d, 0, Duration.Zero)
}

final case class IncreasingUsersPerSecProfileBuilder(usersPerSec: Double) {
  def times(nbOfSteps: Int) = IncreasingUsersPerSecProfileBuilderWithTime(usersPerSec, nbOfSteps)
}

object OpenInjectionSupport {

  val OpenInjectionProfileFactory: InjectionProfileFactory[OpenInjectionStep] =
    (steps: Iterable[OpenInjectionStep]) => OpenInjectionProfile(steps)
}

trait OpenInjectionSupport {

  implicit def openInjectionProfileFactory: InjectionProfileFactory[OpenInjectionStep] =
    OpenInjectionSupport.OpenInjectionProfileFactory

  def rampUsers(users: Int) = RampBuilder(users)
  def heavisideUsers(users: Int) = HeavisideBuilder(users)
  def atOnceUsers(users: Int) = AtOnceOpenInjection(users)

  def constantUsersPerSec(rate: Double) = ConstantRateBuilder(rate)
  def rampUsersPerSec(rate1: Double) = PartialRampRateBuilder(rate1)

  def nothingFor(d: FiniteDuration) = NothingForOpenInjection(d)

  def incrementUsersPerSec(usersPerSec: Double) = IncreasingUsersPerSecProfileBuilder(usersPerSec)
}

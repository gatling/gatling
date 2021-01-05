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

package io.gatling.core.controller.inject.closed

import scala.concurrent.duration._

import io.gatling.core.controller.inject.InjectionProfileFactory

final case class ConstantConcurrentNumberBuilder(number: Int) {

  def during(d: FiniteDuration): ClosedInjectionStep = ConstantConcurrentNumberInjection(number, d)
}

final case class RampConcurrentNumberInjectionFrom(from: Int) {

  def to(t: Int): RampConcurrentNumberInjectionTo = RampConcurrentNumberInjectionTo(from, t)
}

final case class RampConcurrentNumberInjectionTo(from: Int, to: Int) {

  def during(d: FiniteDuration): ClosedInjectionStep = RampConcurrentNumberInjection(from, to, d)
}

final case class IncreasingConcurrentUsersProfileBuilderWithTime(concurrentUsers: Int, nbOfSteps: Int) {
  def eachLevelLasting(levelDuration: FiniteDuration): IncreasingConcurrentUsersCompositeStep =
    IncreasingConcurrentUsersCompositeStep(concurrentUsers, nbOfSteps, levelDuration, 0, Duration.Zero)
}

final case class IncreasingConcurrentUsersProfileBuilder(concurrentUsers: Int) {
  def times(nbOfSteps: Int): IncreasingConcurrentUsersProfileBuilderWithTime = IncreasingConcurrentUsersProfileBuilderWithTime(concurrentUsers, nbOfSteps)
}

object ClosedInjectionSupport {

  val ClosedInjectionProfileFactory: InjectionProfileFactory[ClosedInjectionStep] =
    (steps: Iterable[ClosedInjectionStep]) => new ClosedInjectionProfile(steps)
}

trait ClosedInjectionSupport {

  implicit def closedInjectionProfileFactory: InjectionProfileFactory[ClosedInjectionStep] =
    ClosedInjectionSupport.ClosedInjectionProfileFactory

  def constantConcurrentUsers(number: Int): ConstantConcurrentNumberBuilder = ConstantConcurrentNumberBuilder(number)

  def rampConcurrentUsers(from: Int): RampConcurrentNumberInjectionFrom = RampConcurrentNumberInjectionFrom(from)

  def incrementConcurrentUsers(concurrentUsers: Int): IncreasingConcurrentUsersProfileBuilder = IncreasingConcurrentUsersProfileBuilder(concurrentUsers)
}

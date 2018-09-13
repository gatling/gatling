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

package io.gatling.core.controller.inject.closed

import scala.concurrent.duration._

import io.gatling.core.controller.inject.{ InjectionProfile, InjectionProfileFactory, MetaInjectionProfile }

object ClosedInjectionSupport {

  val ClosedInjectionProfileFactory: InjectionProfileFactory[ClosedInjectionStep] =
    (steps: Iterable[ClosedInjectionStep]) => ClosedInjectionProfile(steps)
}

trait ClosedInjectionSupport extends MetaClosedInjectionSupport {

  implicit def closedInjectionProfileFactory: InjectionProfileFactory[ClosedInjectionStep] =
    ClosedInjectionSupport.ClosedInjectionProfileFactory

  def constantConcurrentUsers(number: Int) = ConstantConcurrentNumberBuilder(number)

  def rampConcurrentUsers(from: Int) = RampConcurrentNumberInjectionFrom(from)
}

case class ConstantConcurrentNumberBuilder(number: Int) {

  def during(d: FiniteDuration) = ConstantConcurrentNumberInjection(number, d)
}

case class RampConcurrentNumberInjectionFrom(from: Int) {

  def to(t: Int) = RampConcurrentNumberInjectionTo(from, t)
}

case class RampConcurrentNumberInjectionTo(from: Int, to: Int) {

  def during(d: FiniteDuration) = RampConcurrentNumberInjection(from, to, d)
}

trait MetaClosedInjectionSupport {

  case class ConcurrentIncreasingTest(
      concurrentUsers: Int,
      nbOfSteps:       Int,
      duration:        FiniteDuration,
      startingUsers:   Int,
      rampDuration:    FiniteDuration
  ) extends MetaInjectionProfile {

    def startingFrom(startingUsers: Int): ConcurrentIncreasingTest = this.copy(startingUsers = startingUsers)
    def separatedByRampsLasting(duration: FiniteDuration): ConcurrentIncreasingTest = this.copy(rampDuration = duration)

    private[inject] def getInjectionSteps: Iterable[ClosedInjectionStep] =
      (1 to nbOfSteps).foldLeft(Iterable.empty[ClosedInjectionStep]) { (acc, currentStep) =>
        val step = if (startingUsers > 0) currentStep - 1 else currentStep
        val newConcurrentUsers = startingUsers + step * concurrentUsers
        val newInjectionSteps = if (currentStep < nbOfSteps && rampDuration > Duration.Zero) {
          val nextConcurrentUsers = newConcurrentUsers + concurrentUsers
          Seq(
            ConstantConcurrentNumberInjection(newConcurrentUsers, duration),
            RampConcurrentNumberInjection(newConcurrentUsers, nextConcurrentUsers, rampDuration)
          )
        } else {
          Seq(ConstantConcurrentNumberInjection(newConcurrentUsers, duration))
        }
        acc ++ newInjectionSteps
      }

    override def profile: InjectionProfile = ClosedInjectionProfile(getInjectionSteps)
  }

  case class ConcurrentIncreasingTestBuilderWithTime(concurrentUsers: Int, nbOfSteps: Int) {
    def eachLevelLasting(d: FiniteDuration) = ConcurrentIncreasingTest(concurrentUsers, nbOfSteps, d, 0, Duration.Zero)
  }

  case class ConcurrentIncreasingTestBuilder(concurrentUsers: Int) {
    def times(nbOfSteps: Int) = ConcurrentIncreasingTestBuilderWithTime(concurrentUsers, nbOfSteps)
  }

  def incrementConcurrentUsers(concurrentUsers: Int) = ConcurrentIncreasingTestBuilder(concurrentUsers)
}

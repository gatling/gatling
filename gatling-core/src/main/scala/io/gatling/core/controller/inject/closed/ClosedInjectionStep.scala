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

package io.gatling.core.controller.inject.closed

import scala.annotation.tailrec
import scala.concurrent.duration._

sealed trait ClosedInjectionStep extends Product with Serializable {

  private[inject] def valueAt(t: FiniteDuration): Int

  private[inject] def duration: FiniteDuration
}

final case class ConstantConcurrentNumberInjection(number: Int, private[inject] val duration: FiniteDuration) extends ClosedInjectionStep {

  require(number >= 0, s"Constant number of concurrent users $number must be >= 0")
  require(duration >= Duration.Zero, s"duration ($duration) must be > 0")

  override private[inject] def valueAt(t: FiniteDuration): Int = {
    require(t <= duration, s"$t must be <= $duration")
    number
  }
}

final case class RampConcurrentNumberInjection(from: Int, to: Int, private[inject] val duration: FiniteDuration) extends ClosedInjectionStep {

  private val durationSeconds = duration.toSeconds

  require(from >= 0, s"Concurrent users ramp from $from must be >= 0")
  require(to >= 0, s"Concurrent users ramp to $to must be >= 0")
  require(duration >= Duration.Zero, s"duration ($duration) must be > 0")

  override private[inject] def valueAt(t: FiniteDuration): Int = {
    require(t <= duration, s"$t must be <= $duration")
    from + math.round((to - from).toDouble / durationSeconds * t.toSeconds).toInt
  }
}

sealed trait CompositeClosedInjectionStepLike extends ClosedInjectionStep {
  private[inject] def composite: CompositeClosedInjectionStep
}

final case class IncreasingConcurrentUsersCompositeStep(
    concurrentUsers: Int,
    nbOfSteps: Int,
    levelDuration: FiniteDuration,
    startingUsers: Int,
    rampDuration: FiniteDuration
) extends CompositeClosedInjectionStepLike {

  def startingFrom(startingUsers: Int): IncreasingConcurrentUsersCompositeStep = this.copy(startingUsers = startingUsers)

  def separatedByRampsLasting(duration: FiniteDuration): IncreasingConcurrentUsersCompositeStep = this.copy(rampDuration = duration)

  override private[inject] lazy val composite: CompositeClosedInjectionStep = {
    val parts = (1 to nbOfSteps).foldLeft(List.empty[ClosedInjectionStep]) { (acc, currentStep) =>
      val step = if (startingUsers > 0) currentStep - 1 else currentStep
      val newConcurrentUsers = startingUsers + step * concurrentUsers
      val newInjectionSteps: List[ClosedInjectionStep] =
        if (currentStep < nbOfSteps && rampDuration > Duration.Zero) {
          val nextConcurrentUsers = newConcurrentUsers + concurrentUsers
          List(
            ConstantConcurrentNumberInjection(newConcurrentUsers, levelDuration),
            RampConcurrentNumberInjection(newConcurrentUsers, nextConcurrentUsers, rampDuration)
          )
        } else {
          List(ConstantConcurrentNumberInjection(newConcurrentUsers, levelDuration))
        }
      acc ++ newInjectionSteps
    }
    CompositeClosedInjectionStep(parts)
  }

  override private[inject] def valueAt(t: FiniteDuration): Int = composite.valueAt(t)

  override private[inject] def duration: FiniteDuration = composite.duration
}

private[inject] final case class CompositeClosedInjectionStep(injectionSteps: List[ClosedInjectionStep]) extends ClosedInjectionStep {

  override private[inject] def valueAt(t: FiniteDuration): Int = {

    @tailrec
    def valueAtRec(time: FiniteDuration, steps: List[ClosedInjectionStep]): Int =
      steps match {
        case Nil => throw new IllegalArgumentException
        case step :: tail =>
          if (time <= step.duration) {
            step.valueAt(time)
          } else {
            valueAtRec(time - step.duration, tail)
          }
      }

    valueAtRec(t, injectionSteps)
  }

  override private[inject] def duration: FiniteDuration = injectionSteps.foldLeft(Duration.Zero) { case (acc, injectionStep) => acc + injectionStep.duration }
}

//[fl]
//
//
//
//
//
//[fl]

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

import scala.annotation.tailrec
import scala.concurrent.duration._

sealed trait ClosedInjectionStep extends Product with Serializable {

  private[inject] def valueAt(t: FiniteDuration): Int

  private[inject] def duration: FiniteDuration

  private[inject] def isEmpty: Boolean
}

final case class ConstantConcurrentNumberInjection private[inject] (number: Int, private[inject] val duration: FiniteDuration) extends ClosedInjectionStep {

  require(number >= 0, s"Constant number of concurrent users $number must be >= 0")
  require(duration >= Duration.Zero, s"duration ($duration) must be > 0")

  override private[inject] def valueAt(t: FiniteDuration): Int = {
    require(t <= duration, s"$t must be <= $duration")
    number
  }

  override private[inject] def isEmpty: Boolean = number == 0
}

final case class RampConcurrentNumberInjection private[inject] (from: Int, to: Int, private[inject] val duration: FiniteDuration) extends ClosedInjectionStep {

  private val slope = (to - from).toDouble / duration.toSeconds

  require(from >= 0, s"Concurrent users ramp from $from must be >= 0")
  require(to >= 0, s"Concurrent users ramp to $to must be >= 0")
  require(duration >= Duration.Zero, s"duration ($duration) must be > 0")

  override private[inject] def valueAt(t: FiniteDuration): Int = {
    require(t <= duration, s"$t must be <= $duration")
    from + (slope * t.toSeconds).toInt
  }

  override private[inject] def isEmpty: Boolean = from == 0 && to == 0
}

sealed trait CompositeClosedInjectionStepLike extends ClosedInjectionStep {
  private[inject] def composite: CompositeClosedInjectionStep
}

final case class IncreasingConcurrentUsersCompositeStep private[inject] (
    concurrentUsers: Int,
    nbOfSteps: Int,
    levelDuration: FiniteDuration,
    startingUsers: Int,
    rampDuration: FiniteDuration
) extends CompositeClosedInjectionStepLike {

  def startingFrom(startingUsers: Int): IncreasingConcurrentUsersCompositeStep = this.copy(startingUsers = startingUsers)

  def separatedByRampsLasting(duration: FiniteDuration): IncreasingConcurrentUsersCompositeStep = this.copy(rampDuration = duration)

  override private[inject] lazy val composite: CompositeClosedInjectionStep = {
    val injectionSteps =
      List.range(0, nbOfSteps).flatMap { stepIdx =>
        if (rampDuration > Duration.Zero) {
          if (startingUsers == 0) {
            // (ramp, level)*
            val rampStartRate = stepIdx * concurrentUsers
            val levelRate = (stepIdx + 1) * concurrentUsers
            RampConcurrentNumberInjection(rampStartRate, levelRate, rampDuration) :: ConstantConcurrentNumberInjection(levelRate, levelDuration) :: Nil

          } else {
            // (level, ramp)* + level
            val levelRate = stepIdx * concurrentUsers + startingUsers
            val level = ConstantConcurrentNumberInjection(levelRate, levelDuration)
            if (stepIdx == nbOfSteps - 1) {
              level :: Nil
            } else {
              val rampEndRate = (stepIdx + 1) * concurrentUsers + startingUsers
              level :: RampConcurrentNumberInjection(levelRate, rampEndRate, rampDuration) :: Nil
            }
          }
        } else {
          // only levels
          val levelRate = stepIdx * concurrentUsers + startingUsers
          ConstantConcurrentNumberInjection(levelRate, levelDuration) :: Nil
        }
      }

    CompositeClosedInjectionStep(injectionSteps)
  }

  override private[inject] def valueAt(t: FiniteDuration): Int = composite.valueAt(t)

  override private[inject] def duration: FiniteDuration = composite.duration

  override private[inject] def isEmpty: Boolean = concurrentUsers == 0 && startingUsers == 0
}

private[inject] final case class CompositeClosedInjectionStep private[inject] (steps: List[ClosedInjectionStep]) extends ClosedInjectionStep {

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

    valueAtRec(t, steps)
  }

  override private[inject] def duration: FiniteDuration = steps.foldLeft(Duration.Zero) { case (acc, injectionStep) => acc + injectionStep.duration }

  override private[inject] def isEmpty: Boolean = steps.forall(_.isEmpty)
}

//[fl]
//
//
//
//
//
//
//
//[fl]

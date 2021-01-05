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

package io.gatling.core.controller.throttle

import scala.annotation.tailrec
import scala.concurrent.duration._

sealed trait ThrottleStep extends Product with Serializable {

  val durationInSec: Long
  def target(previousLastValue: Int): Int
  def rps(time: Long, previousLastValue: Int): Int
}

final case class ReachIntermediate(target: Int) {
  def in(duration: FiniteDuration): ThrottleStep = Reach(target, duration)
}

final case class Reach(target: Int, duration: FiniteDuration) extends ThrottleStep {
  override val durationInSec: Long = duration.toSeconds
  override def target(previousLastValue: Int): Int = target
  override def rps(time: Long, previousLastValue: Int): Int = (previousLastValue + (target - previousLastValue) * (time + 1) / durationInSec).toInt
}

final case class Hold(duration: FiniteDuration) extends ThrottleStep {
  override val durationInSec: Long = duration.toSeconds
  override def target(previousLastValue: Int): Int = previousLastValue
  override def rps(time: Long, previousLastValue: Int): Int = previousLastValue
}

final case class Jump(target: Int) extends ThrottleStep {
  override val durationInSec: Long = 0L
  override def target(previousLastValue: Int): Int = target
  override def rps(time: Long, previousLastValue: Int): Int = 0
}

trait ThrottlingSupport {
  def reachRps(target: Int): ReachIntermediate = ReachIntermediate(target)
  def holdFor(duration: FiniteDuration): ThrottleStep = Hold(duration)
  def jumpToRps(target: Int): ThrottleStep = Jump(target)
}

final case class Throttlings(global: Option[Throttling], perScenario: Map[String, Throttling]) {
  val isEmpty: Boolean = global.isEmpty && perScenario.isEmpty
}

object Throttling {

  @tailrec
  private def valueAt(steps: List[ThrottleStep], pendingTime: Long, previousLastValue: Int): Int = steps match {
    case Nil => 0
    case head :: tail =>
      if (pendingTime < head.durationInSec)
        head.rps(pendingTime, previousLastValue)
      else
        valueAt(tail, pendingTime - head.durationInSec, head.target(previousLastValue))
  }

  def apply(steps: Iterable[ThrottleStep]): Throttling = {

    val reversedSteps = steps.toList
    val limit: Long => Int = valueAt(reversedSteps, _, 0)

    val duration: FiniteDuration =
      steps.foldLeft(Duration.Zero) { (acc, step) =>
        step match {
          case Reach(_, d) => acc + d
          case Hold(d)     => acc + d
          case _           => acc
        }
      }

    Throttling(limit, duration)
  }
}

final case class Throttling(limit: Long => Int, duration: FiniteDuration)

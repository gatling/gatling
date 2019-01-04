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

import scala.concurrent.duration._

sealed trait ClosedInjectionStep {

  def valueAt(t: FiniteDuration): Int

  def duration: FiniteDuration
}

case class ConstantConcurrentNumberInjection(number: Int, duration: FiniteDuration) extends ClosedInjectionStep {

  require(number >= 0, s"Constant number of concurrent users $number must be >= 0")
  require(duration >= Duration.Zero, s"duration ($duration) must be > 0")

  override def valueAt(t: FiniteDuration): Int = {
    require(t <= duration, s"$t must be <= $duration")
    number
  }
}

case class RampConcurrentNumberInjection(from: Int, to: Int, duration: FiniteDuration) extends ClosedInjectionStep {

  private val durationSeconds = duration.toSeconds

  require(from >= 0, s"Concurrent users ramp from $from must be >= 0")
  require(to >= 0, s"Concurrent users ramp to $to must be >= 0")
  require(duration >= Duration.Zero, s"duration ($duration) must be > 0")

  override def valueAt(t: FiniteDuration): Int = {
    require(t <= duration, s"$t must be <= $duration")
    from + math.round((to - from).toDouble / durationSeconds * t.toSeconds).toInt
  }
}

//[fl]
//
//
//
//
//
//[fl]

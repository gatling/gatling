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

package io.gatling.core.pause

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.session.Expression

trait PauseSupport {

  val disabledPauses: PauseType = Disabled
  val constantPauses: PauseType = Constant
  val exponentialPauses: PauseType = Exponential
  def normalPausesWithPercentageDuration(stdDev: Double): PauseType = new NormalWithPercentageDuration(stdDev)
  def normalPausesWithStdDevDuration(stdDev: FiniteDuration): PauseType = new NormalWithStdDevDuration(stdDev)
  def customPauses(custom: Expression[Long]): PauseType = new Custom(custom)
  def uniformPausesPlusOrMinusPercentage(plusOrMinus: Double): PauseType = new UniformPercentage(plusOrMinus)
  def uniformPausesPlusOrMinusDuration(plusOrMinus: FiniteDuration): PauseType = new UniformDuration(plusOrMinus)
}

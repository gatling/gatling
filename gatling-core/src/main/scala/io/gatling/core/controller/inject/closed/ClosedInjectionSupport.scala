/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.controller.inject.InjectionProfileFactory

object ClosedInjectionSupport {

  val ClosedInjectionProfileFactory: InjectionProfileFactory[ClosedInjectionStep] =
    (steps: Iterable[ClosedInjectionStep]) => ClosedInjectionProfile(steps)
}

trait ClosedInjectionSupport {

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

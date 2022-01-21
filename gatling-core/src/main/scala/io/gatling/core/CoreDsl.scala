/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.core

import io.gatling.core.assertion.AssertionSupport
import io.gatling.core.body.BodySupport
import io.gatling.core.check.CheckSupport
import io.gatling.core.controller.inject.closed.ClosedInjectionSupport
import io.gatling.core.controller.inject.open.OpenInjectionSupport
import io.gatling.core.controller.throttle.ThrottlingSupport
import io.gatling.core.feeder.FeederSupport
import io.gatling.core.pause.PauseSupport
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.structure.{ ScenarioBuilder, StructureSupport }

trait CoreDsl
    extends StructureSupport
    with PauseSupport
    with CheckSupport
    with FeederSupport
    with OpenInjectionSupport
    with ClosedInjectionSupport
    with ThrottlingSupport
    with AssertionSupport
    with BodySupport
    with CoreDefaultImplicits
    with ValidationImplicits {

  def scenario(name: String): ScenarioBuilder = new ScenarioBuilder(name.replaceAll("[\r\n\t]", " ").trim, Nil)

  @deprecated("Will be removed in a future release. Please use AllowList instead.", "3.7.0")
  def WhiteList(patterns: String*): io.gatling.core.filter.AllowList = AllowList(patterns: _*)

  def AllowList(patterns: String*): io.gatling.core.filter.AllowList = new io.gatling.core.filter.AllowList(patterns.toList)

  @deprecated("Will be removed in a future release. Please use DenyList instead.", "3.7.0")
  def BlackList(patterns: String*): io.gatling.core.filter.DenyList = DenyList(patterns: _*)

  def DenyList(patterns: String*): io.gatling.core.filter.DenyList = new io.gatling.core.filter.DenyList(patterns.toList)

  @deprecated("Will be removed in a future release. Please use Gatling EL Map key access.", "3.7.0")
  def flattenMapIntoAttributes(map: Expression[Map[String, Any]]): Expression[Session] =
    session => map(session).map(resolvedMap => session.setAll(resolvedMap))
}

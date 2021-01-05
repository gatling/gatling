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

package io.gatling.core.action.builder

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.action.{ Action, Pace }
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext

/**
 * Builder for the Pace action
 *
 * Originally contributed by James Pickering.
 */
class PaceBuilder(interval: Expression[FiniteDuration], counter: String) extends ActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action =
    new Pace(interval, counter, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, next)
}

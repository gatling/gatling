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

import io.gatling.core.action.{ Action, Pause }
import io.gatling.core.pause.{ Disabled, PauseType }
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

/**
 * Builder for the 'pause' action.
 *
 * @constructor create a new PauseBuilder
 * @param duration mean duration of the generated pause
 * @param force if the global pause type has to be overridden
 */
class PauseBuilder(duration: Expression[FiniteDuration], force: Option[PauseType]) extends ActionBuilder with NameGen {

  override def build(ctx: ScenarioContext, next: Action): Action =
    force.getOrElse(ctx.pauseType) match {
      case Disabled => next
      case pauseType =>
        val generator = pauseType.generator(duration)
        new Pause(generator, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, genName("pause"), next)
    }
}

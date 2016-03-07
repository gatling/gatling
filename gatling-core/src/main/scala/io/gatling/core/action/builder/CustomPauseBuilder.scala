/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.core.action.{ Action, Pause }
import io.gatling.core.pause.Disabled
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

/**
 * Builder for the custom 'pause' action.
 *
 * @constructor create a new PauseActionBuilder
 * @param delayGenerator the strategy for computing the duration of the generated pause, in milliseconds
 */
class CustomPauseBuilder(delayGenerator: Expression[Long]) extends ActionBuilder with NameGen {

  override def build(ctx: ScenarioContext, next: Action): Action =
    ctx.pauseType match {
      case Disabled => next
      case _        => new Pause(delayGenerator, ctx.system, ctx.coreComponents.statsEngine, genName("customPause"), next)
    }
}

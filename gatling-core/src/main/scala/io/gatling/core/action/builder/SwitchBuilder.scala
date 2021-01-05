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

import io.gatling.core.action.{ Action, Switch }
import io.gatling.core.session._
import io.gatling.core.structure.{ ChainBuilder, ScenarioContext }
import io.gatling.core.util.NameGen

class SwitchBuilder(value: Expression[Any], possibilities: List[(Any, ChainBuilder)], elseNext: Option[ChainBuilder]) extends ActionBuilder with NameGen {

  require(possibilities.size >= 2, "Switch requires at least 2 possibilities")

  override def build(ctx: ScenarioContext, next: Action): Action = {

    val possibleActions: Map[Any, Action] = possibilities.map { case (value, possibility) =>
      val possibilityAction = possibility.build(ctx, next)
      (value, possibilityAction)
    }.toMap

    val elseNextAction = elseNext.map(_.build(ctx, next)).getOrElse(next)

    val nextAction = value.map(resolvedValue => possibleActions.getOrElse(resolvedValue, elseNextAction))

    new Switch(nextAction, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, genName("switch"), next)
  }
}

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

import io.gatling.core.action.{ Action, If }
import io.gatling.core.session.Expression
import io.gatling.core.structure.{ ChainBuilder, ScenarioContext }

/**
 * @constructor create a new IfBuilder
 * @param condition condition of the if
 * @param thenNext chain that will be executed if condition evaluates to true
 * @param elseNext chain that will be executed if condition evaluates to false
 */
class IfBuilder(condition: Expression[Boolean], thenNext: ChainBuilder, elseNext: Option[ChainBuilder]) extends ActionBuilder {

  def build(ctx: ScenarioContext, next: Action): Action = {
    val safeCondition = condition.safe
    val thenNextAction = thenNext.build(ctx, next)
    val elseNextAction = elseNext.map(_.build(ctx, next)).getOrElse(next)
    new If(safeCondition, thenNextAction, elseNextAction, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, next)
  }
}

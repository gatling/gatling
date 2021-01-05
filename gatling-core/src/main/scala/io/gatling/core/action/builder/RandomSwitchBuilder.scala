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

import io.gatling.commons.validation._
import io.gatling.core.action.{ Action, Switch }
import io.gatling.core.session.Expression
import io.gatling.core.structure.{ ChainBuilder, ScenarioContext }
import io.gatling.core.util.{ NameGen, RandomDistribution }

import com.typesafe.scalalogging.StrictLogging

class RandomSwitchBuilder(possibilities: List[(Double, ChainBuilder)], elseNext: Option[ChainBuilder]) extends ActionBuilder with StrictLogging with NameGen {

  override def build(ctx: ScenarioContext, next: Action): Action = {
    val possibleActions = possibilities.map { case (weight, actionBuilder) => weight -> actionBuilder.build(ctx, next) }
    val fallbackAction = elseNext.map(_.build(ctx, next)).getOrElse(next)
    val randomDistribution = RandomDistribution.percentWeights(possibleActions, fallbackAction)
    val nextAction: Expression[Action] = _ => randomDistribution.next().success
    new Switch(nextAction, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, genName("randomSwitch"), next)
  }
}

class UniformRandomSwitchBuilder(possibilities: List[ChainBuilder]) extends ActionBuilder with StrictLogging with NameGen {

  override def build(ctx: ScenarioContext, next: Action): Action = {
    val possibleActions = possibilities.map(_.build(ctx, next))
    val randomDistribution = RandomDistribution.uniform(possibleActions)
    val nextAction: Expression[Action] = _ => randomDistribution.next().success
    new Switch(nextAction, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, genName("uniformRandomSwitch"), next)
  }
}

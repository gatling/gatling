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

import io.gatling.core.action.{ Action, Loop }
import io.gatling.core.session.Expression
import io.gatling.core.structure.{ ChainBuilder, ScenarioContext }
import io.gatling.core.util.NameGen

sealed abstract class LoopType(val name: String, val timeBased: Boolean)
case object RepeatLoopType extends LoopType("repeat", false)
case object ForeachLoopType extends LoopType("foreach", false)
case object DuringLoopType extends LoopType("during", true)
case object ForeverLoopType extends LoopType("forever", false)
case object AsLongAsLoopType extends LoopType("asLongAs", false)

/**
 * @constructor create a new Loop
 * @param condition the function that determine the condition
 * @param loopNext chain that will be executed if condition evaluates to true
 * @param counterName the name of the loop counter
 * @param exitASAP if the loop is to be exited as soon as the condition no longer holds
 * @param loopType the loop type
 */
class LoopBuilder(condition: Expression[Boolean], loopNext: ChainBuilder, counterName: String, exitASAP: Boolean, loopType: LoopType) extends ActionBuilder with NameGen {

  def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val safeCondition = condition.safe
    val loopAction = new Loop(safeCondition, counterName, exitASAP, loopType.timeBased, coreComponents.statsEngine, genName(loopType.name), next)
    val loopNextAction = loopNext.build(ctx, loopAction)
    loopAction.initialize(loopNextAction, ctx.system)
    loopAction
  }
}

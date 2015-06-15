/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.collection.breakOut

import io.gatling.core.action.Switch
import io.gatling.core.session._
import io.gatling.core.structure.{ ScenarioContext, ChainBuilder }

import akka.actor.ActorRef

class SwitchBuilder(value: Expression[Any], possibilities: List[(Any, ChainBuilder)], elseNext: Option[ChainBuilder]) extends ActionBuilder {

  require(possibilities.size >= 2, "Switch requires at least 2 possibilities")

  def build(ctx: ScenarioContext, next: ActorRef) = {

    val possibleActions: Map[Any, ActorRef] = possibilities.map {
      case (percentage, possibility) =>
        val possibilityAction = possibility.build(ctx, next)
        (percentage, possibilityAction)
    }(breakOut)

    val elseNextActor = elseNext.map(_.build(ctx, next)).getOrElse(next)

    val nextAction = value.map(resolvedValue => possibleActions.getOrElse(resolvedValue, elseNextActor))

    ctx.system.actorOf(Switch.props(nextAction, ctx.coreComponents.statsEngine, next), actorName("switch"))
  }
}

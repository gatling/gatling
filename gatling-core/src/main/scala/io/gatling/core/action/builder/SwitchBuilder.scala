/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.action.builder

import scala.collection.breakOut
import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import io.gatling.core.action.Switch
import io.gatling.core.config.Protocol
import io.gatling.core.session._
import io.gatling.core.structure.{ ScenarioContext, ChainBuilder }

class SwitchBuilder(value: Expression[Any], possibilities: List[(Any, ChainBuilder)], elseNext: Option[ChainBuilder]) extends ActionBuilder {

  require(possibilities.size >= 2, "Switch requires at least 2 possibilities")

  def build(next: ActorRef, ctx: ScenarioContext) = {

    val possibleActions: Map[Any, ActorRef] = possibilities.map {
      case (percentage, possibility) =>
        val possibilityAction = possibility.build(next, ctx)
        (percentage, possibilityAction)
    }(breakOut)

    val elseNextActor = elseNext.map(_.build(next, ctx)).getOrElse(next)

    val nextAction = value.map(resolvedValue => possibleActions.getOrElse(resolvedValue, elseNextActor))

    actor(actorName("switch"))(new Switch(nextAction, next))
  }

  override def defaultProtocols: Set[Protocol] = {

    val actionBuilders = possibilities.flatMap { case (_, chainBuilder) => chainBuilder.actionBuilders } ::: elseNext.map(_.actionBuilders).getOrElse(Nil)
    actionBuilders.flatMap(_.defaultProtocols).toSet
  }
}

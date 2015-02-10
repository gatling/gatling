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

import akka.actor.ActorRef
import akka.actor.ActorDSL.actor
import io.gatling.core.action.Loop
import io.gatling.core.config.Protocols
import io.gatling.core.session.Expression
import io.gatling.core.structure.ChainBuilder

sealed trait LoopType { def name: String }
case object RepeatLoopType extends LoopType { val name = "repeat" }
case object ForeachLoopType extends LoopType { val name = "foreach" }
case object DuringLoopType extends LoopType { val name = "during" }
case object ForeverLoopType extends LoopType { val name = "forever" }
case object AsLongAsLoopType extends LoopType { val name = "asLongAs" }

/**
 * @constructor create a new Loop
 * @param condition the function that determine the condition
 * @param loopNext chain that will be executed if condition evaluates to true
 * @param counterName the name of the loop counter
 * @param exitASAP if the loop is to be exited as soon as the condition no longer holds
 */
class LoopBuilder(condition: Expression[Boolean], loopNext: ChainBuilder, counterName: String, exitASAP: Boolean, loopType: LoopType) extends ActionBuilder {

  def build(next: ActorRef, protocols: Protocols) = {
    val safeCondition = condition.safe
    val whileActor = actor(actorName(loopType.name))(new Loop(safeCondition, counterName, exitASAP, next))
    val loopNextActor = loopNext.build(whileActor, protocols)
    whileActor ! loopNextActor
    whileActor
  }

  override def registerDefaultProtocols(protocols: Protocols) =
    loopNext.actionBuilders.foldLeft(protocols) { (protocols, actionBuilder) =>
      actionBuilder.registerDefaultProtocols(protocols)
    }
}

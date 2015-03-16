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
import io.gatling.core.config.{ Protocol, Protocols }
import io.gatling.core.session.Expression
import io.gatling.core.structure.{ ScenarioContext, ChainBuilder }

sealed abstract class LoopType(val name: String)
case object RepeatLoopType extends LoopType("repeat")
case object ForeachLoopType extends LoopType("foreach")
case object DuringLoopType extends LoopType("during")
case object ForeverLoopType extends LoopType("forever")
case object AsLongAsLoopType extends LoopType("asLongAs")

/**
 * @constructor create a new Loop
 * @param condition the function that determine the condition
 * @param loopNext chain that will be executed if condition evaluates to true
 * @param counterName the name of the loop counter
 * @param exitASAP if the loop is to be exited as soon as the condition no longer holds
 */
class LoopBuilder(condition: Expression[Boolean], loopNext: ChainBuilder, counterName: String, exitASAP: Boolean, loopType: LoopType) extends ActionBuilder {

  def build(next: ActorRef, ctx: ScenarioContext) = {
    val safeCondition = condition.safe
    val whileActor = actor(actorName(loopType.name))(new Loop(safeCondition, counterName, exitASAP, ctx.dataWriters, next))
    val loopNextActor = loopNext.build(whileActor, ctx)
    whileActor ! loopNextActor
    whileActor
  }

  override def defaultProtocols: Set[Protocol] =
    loopNext.actionBuilders.flatMap(_.defaultProtocols).toSet
}

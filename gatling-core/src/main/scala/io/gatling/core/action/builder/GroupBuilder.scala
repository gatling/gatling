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

import akka.actor.{ ActorSystem, ActorRef }
import io.gatling.core.action.{ GroupEnd, GroupStart }
import io.gatling.core.session.Expression
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.structure.ScenarioContext

object GroupBuilder {

  def start(groupName: Expression[String]) = new ActionBuilder {
    def build(system: ActorSystem, ctx: ScenarioContext, protocolComponentsRegistry: ProtocolComponentsRegistry, next: ActorRef) =
      system.actorOf(GroupStart.props(groupName, ctx.coreComponents.statsEngine, next), actorName("groupStart"))
  }

  val End = new ActionBuilder {
    def build(system: ActorSystem, ctx: ScenarioContext, protocolComponentsRegistry: ProtocolComponentsRegistry, next: ActorRef) =
      system.actorOf(GroupEnd.props(ctx.coreComponents.statsEngine, next), actorName("groupEnd"))
  }
}

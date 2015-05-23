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

import scala.collection.mutable

import akka.actor.{ ActorSystem, ActorRef }
import io.gatling.core.action.{ Feed, SingletonFeed }
import io.gatling.core.feeder.FeederBuilder
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.session.Expression

object FeedBuilder {

  // FIXME not very clean, but is there a better way?
  val Instances = mutable.Map.empty[FeederBuilder[_], ActorRef]
}

class FeedBuilder(feederBuilder: FeederBuilder[_], number: Expression[Int]) extends ActionBuilder {

  def build(system: ActorSystem, ctx: ScenarioContext, protocolComponentsRegistry: ProtocolComponentsRegistry, next: ActorRef) = {

    val feederInstance = FeedBuilder.Instances.getOrElseUpdate(feederBuilder, system.actorOf(SingletonFeed.props(feederBuilder.build(system)), actorName("singletonFeed")))

    system.actorOf(Feed.props(feederInstance, ctx.coreComponents.controller, number, ctx.coreComponents.statsEngine, next), actorName("feed"))
  }
}

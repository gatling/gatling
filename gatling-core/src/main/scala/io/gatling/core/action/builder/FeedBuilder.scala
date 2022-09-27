/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import java.{ util => ju }

import scala.jdk.CollectionConverters._

import io.gatling.core.action.{ Action, Feed, FeedActor }
import io.gatling.core.feeder.{ FeederBuilder, SourceFeederBuilder }
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

import akka.actor.ActorRef

private[core] object FeedBuilder {
  private val Instances = new ju.HashMap[Long, ActorRef].asScala
}

private[core] final class FeedBuilder(feederBuilder: FeederBuilder, feederBuilderKey: Long, number: Option[Expression[Int]])
    extends ActionBuilder
    with NameGen {

  private def newFeedActor(ctx: ScenarioContext): ActorRef = {
    val feederName = feederBuilder match {
      case sourceFeederBuilder: SourceFeederBuilder[_] => Some(sourceFeederBuilder.source.name)
      case _                                           => None
    }

    val props = FeedActor.props(feederBuilder(), feederName, ctx.coreComponents.controller)
    ctx.coreComponents.actorSystem.actorOf(props, genName("feed"))
  }

  override def build(ctx: ScenarioContext, next: Action): Action = {
    val feedActor = FeedBuilder.Instances.getOrElseUpdate(feederBuilderKey, newFeedActor(ctx))
    new Feed(feedActor, number, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, next)
  }
}

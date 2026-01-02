/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import io.gatling.core.action.{ Action, Feed, FeedActor, FeedMessage }
import io.gatling.core.actor.ActorRef
import io.gatling.core.feeder.{ FeederBuilder, NamedFeederBuilder }
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

private[core] object FeedBuilder {
  private val Instances = new ju.HashMap[Long, ActorRef[FeedMessage]].asScala
}

private[core] final class FeedBuilder(
    feederBuilder: FeederBuilder,
    feederBuilderKey: Long,
    number: Option[Expression[Int]],
    generateJavaCollection: Boolean,
    feedCallSite: Option[String]
) extends ActionBuilder
    with NameGen {
  private def newFeedActor(ctx: ScenarioContext): ActorRef[FeedMessage] = {
    val feederName = feederBuilder match {
      case namedFeederBuilder: NamedFeederBuilder => Some(namedFeederBuilder.name)
      case _                                      => None
    }

    val feeder = feederBuilder()

    feeder match {
      case closeable: AutoCloseable => ctx.coreComponents.actorSystem.registerOnTermination(closeable.close())
      case _                        =>
    }

    val props = FeedActor.actor(feeder, genName("feed"), feederName, generateJavaCollection, ctx.coreComponents.controller, feedCallSite)
    ctx.coreComponents.actorSystem.actorOf(props)
  }

  override def build(ctx: ScenarioContext, next: Action): Action = {
    val feedActor = FeedBuilder.Instances.getOrElseUpdate(feederBuilderKey, newFeedActor(ctx))
    new Feed(feedActor, number, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, next)
  }
}

/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.collection.mutable

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import io.gatling.core.action.{ Feed, SingletonFeed }
import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.config.ProtocolRegistry
import io.gatling.core.feeder.FeederBuilder
import io.gatling.core.session.Expression

object FeedBuilder extends AkkaDefaults {

	// FIXME not very clean
	val instances = mutable.Map.empty[FeederBuilder[_], ActorRef]

	def apply[T](feederBuilder: FeederBuilder[T], number: Expression[Int]) =
		new FeedBuilder(instances.getOrElseUpdate(feederBuilder, actor(new SingletonFeed(feederBuilder.build))), number)
}
class FeedBuilder(instance: => ActorRef, number: Expression[Int]) extends ActionBuilder {

	private[gatling] def build(next: ActorRef, protocolRegistry: ProtocolRegistry) = actor(new Feed(instance, number, next))
}

/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.action.builder

import scala.collection.mutable

import com.excilys.ebi.gatling.core.action.{ Feed, SingletonFeed, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.feeder.FeederBuilder
import com.excilys.ebi.gatling.core.session.Expression

import akka.actor.{ ActorRef, Props }

object FeedBuilder {

	val instances = mutable.Map.empty[FeederBuilder[_], ActorRef]

	def apply[T](feeder: FeederBuilder[T], number: Expression[Int]) = {
		def newInstance = system.actorOf(Props(new SingletonFeed(feeder.build, number)))

		new FeedBuilder(instances.getOrElseUpdate(feeder, newInstance))
	}
}
class FeedBuilder(instance: ActorRef) extends ActionBuilder {

	/**
	 * @param protocolConfigurationRegistry
	 * @param next the Action that will be chained with the Action build by this builder
	 * @return the built Action
	 */
	private[gatling] def build(next: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry) =
		system.actorOf(Props(new Feed(instance, next)))
}

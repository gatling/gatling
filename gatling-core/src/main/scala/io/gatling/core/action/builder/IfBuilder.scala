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

import io.gatling.core.action.{ If, system }
import io.gatling.core.session.Expression
import io.gatling.core.structure.ChainBuilder
import akka.actor.{ ActorRef, Props }
import io.gatling.core.config.ProtocolRegistry

/**
 * @constructor create a new IfBuilder
 * @param condition condition of the if
 * @param thenNext chain that will be executed if condition evaluates to true
 * @param elseNext chain that will be executed if condition evaluates to false
 */
class IfBuilder(condition: Expression[Boolean], thenNext: ChainBuilder, elseNext: Option[ChainBuilder]) extends ActionBuilder {

	def build(next: ActorRef, protocolRegistry: ProtocolRegistry) = {
		val thenNextActor = thenNext.build(next, protocolRegistry)
		val elseNextActor = elseNext.map(_.build(next, protocolRegistry)).getOrElse(next)
		system.actorOf(Props(new If(condition, thenNextActor, elseNextActor, next)))
	}
}

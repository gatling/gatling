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

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import io.gatling.core.action.If
import io.gatling.core.config.Protocols
import io.gatling.core.session.Expression
import io.gatling.core.structure.ChainBuilder
/**
 * @constructor create a new IfBuilder
 * @param condition condition of the if
 * @param thenNext chain that will be executed if condition evaluates to true
 * @param elseNext chain that will be executed if condition evaluates to false
 */
class IfBuilder(condition: Expression[Boolean], thenNext: ChainBuilder, elseNext: Option[ChainBuilder]) extends ActionBuilder {

	def build(next: ActorRef, protocols: Protocols) = {
		val thenNextActor = thenNext.build(next, protocols)
		val elseNextActor = elseNext.map(_.build(next, protocols)).getOrElse(next)
		actor(new If(condition, thenNextActor, elseNextActor, next))
	}

	override def registerDefaultProtocols(protocols: Protocols) = {

		val actionBuilders = thenNext.actionBuilders ::: elseNext.map(_.actionBuilders).getOrElse(Nil)

		actionBuilders.foldLeft(protocols) { (protocols, actionBuilder) =>
			actionBuilder.registerDefaultProtocols(protocols)
		}
	}
}

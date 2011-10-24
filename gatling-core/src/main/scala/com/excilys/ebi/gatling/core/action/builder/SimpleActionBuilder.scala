/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.Action
import scala.collection.immutable.List
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.action.SimpleAction
import akka.actor.TypedActor

object SimpleActionBuilder {
	implicit def toSimpleActionBuilder(contextModifier: (Context, Action) => Unit) = new SimpleActionBuilder(contextModifier, null, Nil)
	implicit def toSimpleActionBuilder(contextModifier: Context => Unit): SimpleActionBuilder = toSimpleActionBuilder((c: Context, a: Action) => contextModifier(c))

	def simpleActionBuilder(contextModifier: Context => Unit): SimpleActionBuilder = simpleActionBuilder((c: Context, a: Action) => contextModifier(c))
	def simpleActionBuilder(contextModifier: (Context, Action) => Unit) = toSimpleActionBuilder(contextModifier)
}

class SimpleActionBuilder(contextModifier: (Context, Action) => Unit, next: Action, groups: List[String]) extends AbstractActionBuilder {

	def withNext(next: Action): AbstractActionBuilder = new SimpleActionBuilder(contextModifier, next, groups)

	def inGroups(groups: List[String]): AbstractActionBuilder = new SimpleActionBuilder(contextModifier, next, groups)

	def build(): Action = {
		logger.debug("Building Simple Action")

		TypedActor.newInstance(classOf[Action], new SimpleAction(contextModifier, next))
	}

}
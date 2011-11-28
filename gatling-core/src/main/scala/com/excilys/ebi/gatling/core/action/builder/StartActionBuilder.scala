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
import akka.actor.TypedActor
import com.excilys.ebi.gatling.core.action.StartAction

/**
 * StartActionBuilder class companion
 */
object StartActionBuilder {
	/**
	 * Creates a new StartActionBuilder
	 *
	 * @return A StartActionBuilder ready to use
	 */
	def startActionBuilder = new StartActionBuilder(null, Nil)
}

/**
 * Builder for StartAction
 *
 * @constructor create a StartActionBuilder with its next action
 * @param next the action to be executed after this one
 * @param groups the groups to which this action belongs
 */
class StartActionBuilder(next: Action, groups: List[String]) extends AbstractActionBuilder {
	def withNext(next: Action) = new StartActionBuilder(next, groups)

	def inGroups(groups: List[String]) = new StartActionBuilder(next, groups)

	def build: Action = TypedActor.newInstance(classOf[Action], new StartAction(next))
}
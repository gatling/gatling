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

/**
 * SimpleActionBuilder class companion
 */
object SimpleActionBuilder {

	/**
	 * Implicit converter from (Context, Action) => Unit to a simple action builder containing this function
	 *
	 * @param contextFunction the function that has to be wrapped into a simple action builder
	 * @return a simple action builder
	 */
	implicit def toSimpleActionBuilder(contextFunction: (Context, Action) => Unit) = simpleActionBuilder(contextFunction)
	/**
	 * Implicit converter from Context => Unit to a simple action builder containing this function
	 *
	 * @param contextFunction the function that has to be wrapped into a simple action builder
	 */
	implicit def toSimpleActionBuilder(contextFunction: Context => Unit) = simpleActionBuilder(contextFunction)

	/**
	 * Function used to create a simple action builder
	 *
	 * @param contextFunction the function that will be executed by the built simple action
	 */
	def simpleActionBuilder(contextFunction: Context => Unit): SimpleActionBuilder = simpleActionBuilder((c: Context, a: Action) => contextFunction(c))
	/**
	 * Function used to create a simple action builder
	 *
	 * @param contextFunction the function that will be executed by the built simple action
	 */
	def simpleActionBuilder(contextFunction: (Context, Action) => Unit) = new SimpleActionBuilder(contextFunction, null, Nil)
}
/**
 * This class builds an SimpleAction
 *
 * @constructor creates a SimpleActionBuilder
 * @param contextFunction the function that will be executed by the simple action
 * @param next the action that will be executed after the simple action built by this builder
 * @param groups the groups to which this action belongs
 */
class SimpleActionBuilder(contextFunction: (Context, Action) => Unit, next: Action, groups: List[String]) extends AbstractActionBuilder {

	def withNext(next: Action) = new SimpleActionBuilder(contextFunction, next, groups)

	def inGroups(groups: List[String]) = new SimpleActionBuilder(contextFunction, next, groups)

	def build(): Action = {
		logger.debug("Building Simple Action")
		TypedActor.newInstance(classOf[Action], new SimpleAction(contextFunction, next))
	}
}
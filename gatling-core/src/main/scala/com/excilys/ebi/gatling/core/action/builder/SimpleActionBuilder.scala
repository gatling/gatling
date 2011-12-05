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
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.action.SimpleAction
import akka.actor.TypedActor

/**
 * SimpleActionBuilder class companion
 */
object SimpleActionBuilder {

	/**
	 * Implicit converter from (Session, Action) => Unit to a simple action builder containing this function
	 *
	 * @param sessionFunction the function that has to be wrapped into a simple action builder
	 * @return a simple action builder
	 */
	implicit def toSimpleActionBuilder(sessionFunction: (Session, Action) => Unit) = simpleActionBuilder(sessionFunction)
	/**
	 * Implicit converter from Session => Unit to a simple action builder containing this function
	 *
	 * @param sessionFunction the function that has to be wrapped into a simple action builder
	 */
	implicit def toSimpleActionBuilder(sessionFunction: Session => Unit) = simpleActionBuilder(sessionFunction)

	/**
	 * Function used to create a simple action builder
	 *
	 * @param sessionFunction the function that will be executed by the built simple action
	 */
	def simpleActionBuilder(sessionFunction: Session => Unit): SimpleActionBuilder = simpleActionBuilder((s: Session, a: Action) => sessionFunction(s))
	/**
	 * Function used to create a simple action builder
	 *
	 * @param sessionFunction the function that will be executed by the built simple action
	 */
	def simpleActionBuilder(sessionFunction: (Session, Action) => Unit) = new SimpleActionBuilder(sessionFunction, null, Nil)
}
/**
 * This class builds an SimpleAction
 *
 * @constructor creates a SimpleActionBuilder
 * @param sessionFunction the function that will be executed by the simple action
 * @param next the action that will be executed after the simple action built by this builder
 * @param groups the groups to which this action belongs
 */
class SimpleActionBuilder(sessionFunction: (Session, Action) => Unit, next: Action, groups: List[String]) extends AbstractActionBuilder {

	def withNext(next: Action) = new SimpleActionBuilder(sessionFunction, next, groups)

	def inGroups(groups: List[String]) = new SimpleActionBuilder(sessionFunction, next, groups)

	def build(): Action = TypedActor.newInstance(classOf[Action], new SimpleAction(sessionFunction, next))
}
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
import com.excilys.ebi.gatling.core.action.IfAction
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.structure.ChainBuilder

/**
 * Companion Object of IfActionBuilder class
 */
object IfActionBuilder {

	/**
	 * Creates an initialized IfActionBuilder
	 */
	def ifActionBuilder = new IfActionBuilder(null, null, null, null, Nil)
}

/**
 * This class builds an IfAction
 *
 * @constructor create a new IfActionBuilder
 * @param conditionFunction condition of the if
 * @param thenNext chain that will be executed if conditionFunction evaluates to true
 * @param elseNext chain that will be executed if conditionFunction evaluates to false
 * @param next chain that will be executed if conditionFunction evaluates to false and there is no elseNext
 * @param groups groups in which this action and the ones inside will be
 */
class IfActionBuilder(conditionFunction: Context => Boolean, thenNext: ChainBuilder, elseNext: Option[ChainBuilder], next: Action, groups: List[String])
		extends AbstractActionBuilder {

	/**
	 * Adds conditionFunction to builder
	 *
	 * @param conditionFunction the condition function
	 * @return a new builder with conditionFunction set
	 */
	def withConditionFunction(conditionFunction: Context => Boolean) = new IfActionBuilder(conditionFunction, thenNext, elseNext, next, groups)

	/**
	 * Adds thenNext to builder
	 *
	 * @param thenNext the chain executed if conditionFunction evaluated to true
	 * @return a new builder with thenNext set
	 */
	def withThenNext(thenNext: ChainBuilder) = new IfActionBuilder(conditionFunction, thenNext, elseNext, next, groups)

	/**
	 * Adds elseNext to builder
	 *
	 * @param elseNext the chain executed if conditionFunction evaluated to false
	 * @return a new builder with elseNext set
	 */
	def withElseNext(elseNext: Option[ChainBuilder]) = new IfActionBuilder(conditionFunction, thenNext, elseNext, next, groups)

	def withNext(next: Action) = new IfActionBuilder(conditionFunction, thenNext, elseNext, next, groups)

	def inGroups(groups: List[String]) = new IfActionBuilder(conditionFunction, thenNext, elseNext, next, groups)

	def build: Action = {
		logger.debug("Building IfAction")

		val actionTrue = thenNext.withNext(next).inGroups(groups).build
		val actionFalse = elseNext.map(_.withNext(next).inGroups(groups).build)

		TypedActor.newInstance(classOf[Action], new IfAction(conditionFunction, actionTrue, actionFalse, next))
	}
}
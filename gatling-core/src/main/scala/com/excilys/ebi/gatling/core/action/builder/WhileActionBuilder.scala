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
import com.excilys.ebi.gatling.core.context.Context
import akka.actor.TypedActor
import com.excilys.ebi.gatling.core.action.WhileAction
import akka.actor.Actor
import com.excilys.ebi.gatling.core.structure.ChainBuilder

/**
 * Companion of the WhileActionBuilder class
 */
object WhileActionBuilder {
	/**
	 * Creates an initialized WhileActionBuilder
	 */
	def whileActionBuilder = new WhileActionBuilder(null, null, null, None, Nil)
}

/**
 * This class builds a WhileActionBuilder
 *
 * @constructor create a new WhileAction
 * @param conditionFunction the function that determine the condition
 * @param loopNext chain that will be executed if conditionFunction evaluates to true
 * @param next action that will be executed if conditionFunction evaluates to false
 * @param groups groups in which this action and the others inside will be
 */
class WhileActionBuilder(conditionFunction: (Context, Action) => Boolean, loopNext: ChainBuilder, next: Action, counterName: Option[String], groups: List[String])
		extends AbstractActionBuilder {

	/**
	 * Adds conditionFunction to this builder
	 *
	 * @param conditionFunction the condition function
	 * @return a new builder with conditionFunction set
	 */
	def withConditionFunction(conditionFunction: Context => Boolean): WhileActionBuilder = withConditionFunction((c: Context, a: Action) => conditionFunction(c))
	/**
	 * Adds conditionFunction to this builder
	 *
	 * @param conditionFunction the condition function
	 * @return a new builder with conditionFunction set
	 */
	def withConditionFunction(conditionFunction: (Context, Action) => Boolean) = new WhileActionBuilder(conditionFunction, loopNext, next, counterName, groups)
	/**
	 * Adds loopNext to builder
	 *
	 * @param loopNext the chain executed if testFunction evaluated to true
	 * @return a new builder with loopNext set
	 */
	def withLoopNext(loopNext: ChainBuilder) = new WhileActionBuilder(conditionFunction, loopNext, next, counterName, groups)
	/**
	 * Adds counterName to builder
	 *
	 * @param counterName the name of the counter that will be used
	 * @return a new builder with counterName set to None or Some(name)
	 */
	def withCounterName(counterName: Option[String]) = new WhileActionBuilder(conditionFunction, loopNext, next, counterName, groups)

	def withNext(next: Action) = new WhileActionBuilder(conditionFunction, loopNext, next, counterName, groups)

	def inGroups(groups: List[String]) = new WhileActionBuilder(conditionFunction, loopNext, next, counterName, groups)

	def build: Action = {
		logger.debug("Building IfAction")
		TypedActor.newInstance(classOf[Action], new WhileAction(conditionFunction, (w: WhileAction) => loopNext.withNext(w).inGroups(groups).build, next, counterName))
	}
}
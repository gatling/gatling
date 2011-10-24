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
import com.excilys.ebi.gatling.core.structure.builder.ChainBuilder

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
 * @param testFunction condition of the if
 * @param nextTrue chain that will be executed if testFunction evaluates to true
 * @param nextFalse chain that will be executed if testFunction evaluates to false
 * @param next chain that will be executed if testFunction evaluates to false and there is no nextFalse
 * @param groups groups in which this action and the ones inside will be
 */
class IfActionBuilder(val testFunction: Context => Boolean, val nextTrue: ChainBuilder, val nextFalse: Option[ChainBuilder],
	val next: Action, val groups: List[String])
		extends AbstractActionBuilder {

	/**
	 * Adds testFunction to builder
	 *
	 * @param testFunction the test function
	 * @return a new builder with testFunction set
	 */
	def withTestFunction(testFunction: Context => Boolean) = new IfActionBuilder(testFunction, nextTrue, nextFalse, next, groups)

	/**
	 * Adds nextTrue to builder
	 *
	 * @param nextTrue the chain executed if testFunction evaluated to true
	 * @return a new builder with nextTrue set
	 */
	def withNextTrue(nextTrue: ChainBuilder) = new IfActionBuilder(testFunction, nextTrue, nextFalse, next, groups)

	/**
	 * Adds nextFalse to builder
	 *
	 * @param nextFalse the chain executed if testFunction evaluated to false
	 * @return a new builder with nextFalse set
	 */
	def withNextFalse(nextFalse: Option[ChainBuilder]) = new IfActionBuilder(testFunction, nextTrue, nextFalse, next, groups)

	def withNext(next: Action) = new IfActionBuilder(testFunction, nextTrue, nextFalse, next, groups)

	def inGroups(groups: List[String]) = new IfActionBuilder(testFunction, nextTrue, nextFalse, next, groups)

	def build: Action = {
		logger.debug("Building IfAction")

		val actionTrue = nextTrue.withNext(next).inGroups(groups).build
		val actionFalse = nextFalse.map { chain =>
			chain.withNext(next).inGroups(groups).build
		}

		TypedActor.newInstance(classOf[Action], new IfAction(testFunction, actionTrue, actionFalse, next))
	}
}
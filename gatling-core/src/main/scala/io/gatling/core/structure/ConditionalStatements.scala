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
package io.gatling.core.structure

import io.gatling.core.action.builder.{ IfBuilder, RandomSwitchBuilder, RoundRobinSwitchBuilder, SwitchBuilder }
import io.gatling.core.session.{ Expression, RichExpression, Session }

trait ConditionalStatements[B] extends Execs[B] {

	/**
	 * Method used to add a conditional execution in the scenario
	 *
	 * @param condition the function that will determine if the condition is satisfied or not
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIf(condition: Expression[Boolean])(thenNext: ChainBuilder): B = doIf(condition, thenNext, None)

	/**
	 * Method used to add a conditional execution in the scenario
	 *
	 * @param sessionKey the key of the session value to be tested for equality
	 * @param value the value to which the session value must be equals
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIf(sessionKey: Expression[String], value: String)(thenNext: ChainBuilder): B = doIf(sessionKey.map(_ == value), thenNext, None)

	/**
	 * Method used to add a conditional execution in the scenario with a fall back
	 * action if condition is not satisfied
	 *
	 * @param condition the function that will determine if the condition is satisfied or not
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @param elseNext the chain to be executed if the condition is not satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIfOrElse(condition: Expression[Boolean])(thenNext: ChainBuilder)(elseNext: ChainBuilder): B = doIf(condition, thenNext, Some(elseNext))

	/**
	 * Method used to add a conditional execution in the scenario with a fall back
	 * action if condition is not satisfied
	 *
	 * @param expected the expected value
	 * @param actual the real value
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @param elseNext the chain to be executed if the condition is not satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIfEqualsOrElse(expected: Expression[Any], actual: Expression[Any])(thenNext: ChainBuilder)(elseNext: ChainBuilder): B = {
		val condition = (session: Session) =>
			for {
				expected <- expected(session)
				actual <- actual(session)
			} yield (expected == actual)

		doIf(condition, thenNext, Some(elseNext))
	}

	/**
	 * Private method that actually adds the If Action to the scenario
	 *
	 * @param condition the function that will determine if the condition is satisfied or not
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @param elseNext the chain to be executed if the condition is not satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	private def doIf(condition: Expression[Boolean], thenNext: ChainBuilder, elseNext: Option[ChainBuilder]): B =
		exec(new IfBuilder(condition, thenNext, elseNext))

	def doSwitch(value: Expression[Any])(possibility1: (Any, ChainBuilder), possibility2: (Any, ChainBuilder), possibilities: (Any, ChainBuilder)*): B =
		doSwitch(value, possibility1 :: possibility2 :: possibilities.toList, None)

	def doSwitchOrElse(value: Expression[Any])(possibility1: (Any, ChainBuilder), possibility2: (Any, ChainBuilder), possibilities: (Any, ChainBuilder)*)(elseNext: ChainBuilder): B =
		doSwitch(value, possibility1 :: possibility2 :: possibilities.toList, Some(elseNext))

	private def doSwitch(value: Expression[Any], possibilities: List[(Any, ChainBuilder)], elseNext: Option[ChainBuilder]): B =
		exec(new SwitchBuilder(value, possibilities, elseNext))

	/**
	 * Add a switch in the chain. Every possible subchain is defined with a percentage.
	 * Switch is selected randomly. If no switch is selected (ie random number exceeds percentages sum), switch is bypassed.
	 * Percentages sum can't exceed 100%.
	 *
	 * @param possibility1 a possible subchain
	 * @param possibilities the rest of the possible subchains
	 * @return a new builder with a random switch added to its actions
	 */
	def randomSwitch(possibility1: (Double, ChainBuilder), possibilities: (Double, ChainBuilder)*): B =
		randomSwitch(possibility1 :: possibilities.toList, None)

	def randomSwitchOrElse(possibility1: (Double, ChainBuilder), possibilities: (Double, ChainBuilder)*)(elseNext: ChainBuilder): B =
		randomSwitch(possibility1 :: possibilities.toList, Some(elseNext))

	private def randomSwitch(possibilities: List[(Double, ChainBuilder)], elseNext: Option[ChainBuilder]): B =
		exec(new RandomSwitchBuilder(possibilities, elseNext))

	/**
	 * Add a switch in the chain. Selection uses a random strategy
	 *
	 * @param possibility1 the first possible subchain
	 * @param possibility2 the second possible subchain
	 * @param possibilities the rest of the possible subchains
	 * @return a new builder with a random switch added to its actions
	 */
	def randomSwitch(possibility1: ChainBuilder, possibility2: ChainBuilder, possibilities: ChainBuilder*): B = {

		val tailPossibilities = possibility2 :: possibilities.toList
		val basePercentage = 100d / (tailPossibilities.size + 1)
		val firstPercentage = 100d - basePercentage * tailPossibilities.size

		val possibilitiesWithPercentage = (firstPercentage, possibility1) :: tailPossibilities.map((basePercentage, _))

		randomSwitch(possibilitiesWithPercentage, None)
	}

	/**
	 * Add a switch in the chain. Selection uses a round robin strategy
	 *
	 * @param possibility1 a possible subchain
	 * @param possibilities the rest of the possible subchains
	 * @return a new builder with a random switch added to its actions
	 */
	def roundRobinSwitch(possibility1: ChainBuilder, possibilities: ChainBuilder*): B =
		exec(new RoundRobinSwitchBuilder(possibility1 :: possibilities.toList))
}

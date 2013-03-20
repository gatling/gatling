/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.structure

import com.excilys.ebi.gatling.core.action.builder.{ IfBuilder, RandomSwitchBuilder, RoundRobinSwitchBuilder }
import com.excilys.ebi.gatling.core.session.Expression

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
	def doIf(sessionKey: Expression[String], value: String)(thenNext: ChainBuilder): B = doIf(session => sessionKey(session).map(_ == value), thenNext, None)

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
	 * @param sessionKey the key of the session value to be tested for equality
	 * @param value the value to which the session value must be equals
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @param elseNext the chain to be executed if the condition is not satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIfOrElse(sessionKey: Expression[String], value: String)(thenNext: ChainBuilder)(elseNext: ChainBuilder): B = doIf(session => sessionKey(session).map(_ == value), thenNext, Some(elseNext))

	/**
	 * Private method that actually adds the If Action to the scenario
	 *
	 * @param condition the function that will determine if the condition is satisfied or not
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @param elseNext the chain to be executed if the condition is not satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	private def doIf(condition: Expression[Boolean], thenNext: ChainBuilder, elseNext: Option[ChainBuilder]): B = newInstance(new IfBuilder(condition = condition, thenNext = thenNext, elseNext = elseNext) :: actionBuilders)

	/**
	 * Add a switch in the chain. Every possible subchain is defined with a percentage.
	 * Switch is selected randomly. If no switch is selected (ie random number exceeds percentages sum), switch is bypassed.
	 * Percentages sum can't exceed 100%.
	 *
	 * @param possibility1 a possible subchain
	 * @param possibilities the rest of the possible subchains
	 * @return a new builder with a random switch added to its actions
	 */
	def randomSwitch(possibility1: (Int, ChainBuilder), possibilities: (Int, ChainBuilder)*): B = newInstance(new RandomSwitchBuilder(possibility1 :: possibilities.toList) :: actionBuilders)

	/**
	 * Add a switch in the chain. Selection uses a random strategy
	 *
	 * @param possibility1 a possible subchain
	 * @param possibilities the rest of the possible subchains
	 * @return a new builder with a random switch added to its actions
	 */
	def randomSwitch(possibility1: ChainBuilder, possibilities: ChainBuilder*): B = {

		val basePercentage = 100 / (possibilities.size + 1)
		val firstPercentage = 100 - basePercentage * possibilities.size

		val possibilitiesWithPercentage = (firstPercentage, possibility1) :: possibilities.toList.map((basePercentage, _))

		newInstance(new RandomSwitchBuilder(possibilitiesWithPercentage) :: actionBuilders)
	}

	/**
	 * Add a switch in the chain. Selection uses a round robin strategy
	 *
	 * @param possibility1 a possible subchain
	 * @param possibilities the rest of the possible subchains
	 * @return a new builder with a random switch added to its actions
	 */
	def roundRobinSwitch(possibility1: ChainBuilder, possibilities: ChainBuilder*): B = newInstance(new RoundRobinSwitchBuilder(possibility1 :: possibilities.toList) :: actionBuilders)

}
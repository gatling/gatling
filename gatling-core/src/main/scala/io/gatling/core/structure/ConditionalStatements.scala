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

  /**
   * Add a switch in the chain. Every possible subchain is defined with a key.
   * Switch is selected through the matching of a key with the evaluation of the passed expression.
   * If no switch is selected, switch is bypassed.
   *
   * @param value expression to evaluate and match to find the right subchain
   * @param possibilities tuples of key and subchain
   * @return a new builder with a switch added to its actions
   */
  def doSwitch(value: Expression[Any])(possibilities: (Any, ChainBuilder)*): B = {
    require(possibilities.size >= 2, "doSwitch()() requires at least 2 possibilities")
    doSwitch(value, possibilities.toList, None)
  }

  /**
   * Add a switch in the chain. Every possible subchain is defined with a key.
   * Switch is selected through the matching of a key with the evaluation of the passed expression.
   * If no switch is selected, the fallback subchain is used.
   *
   * @param value expression to evaluate and match to find the right subchain
   * @param possibilities tuples of key and subchain
   * @param elseNext fallback subchain
   * @return a new builder with a switch added to its actions
   */
  def doSwitchOrElse(value: Expression[Any])(possibilities: (Any, ChainBuilder)*)(elseNext: ChainBuilder): B = {
    require(possibilities.size >= 2, "doSwitchOrElse()()() requires at least 2 possibilities")
    doSwitch(value, possibilities.toList, Some(elseNext))
  }

  private def doSwitch(value: Expression[Any], possibilities: List[(Any, ChainBuilder)], elseNext: Option[ChainBuilder]): B =
    exec(new SwitchBuilder(value, possibilities, elseNext))

  /**
   * Add a switch in the chain. Every possible subchain is defined with a percentage.
   * Switch is selected randomly. If no switch is selected (ie: random number exceeds percentages sum), switch is bypassed.
   * Percentages sum can't exceed 100%.
   *
   * @param possibilities the possible subchains
   * @return a new builder with a random switch added to its actions
   */
  def randomSwitch(possibilities: (Double, ChainBuilder)*): B = {
    require(possibilities.size >= 1, "randomSwitch() requires at least 1 possibility")
    randomSwitch(possibilities.toList, None)
  }

  /**
   * Add a switch in the chain. Every possible subchain is defined with a percentage.
   * Switch is selected randomly. If no switch is selected (ie: random number exceeds percentages sum),
   * the subchain defined as the fallback will be used.
   * Percentages sum must be below 100%.
   *
   * @param possibilities the possible subchains
   * @param elseNext fallback subchain
   * @return a new builder with a random switch added to its actions
   */
  def randomSwitchOrElse(possibilities: (Double, ChainBuilder)*)(elseNext: ChainBuilder): B = {
    require(possibilities.size >= 1, "randomSwitchOrElse() requires at least 1 possibility")
    randomSwitch(possibilities.toList, Some(elseNext))
  }

  private def randomSwitch(possibilities: List[(Double, ChainBuilder)], elseNext: Option[ChainBuilder]): B =
    exec(new RandomSwitchBuilder(possibilities, elseNext))

  /**
   * Add a switch in the chain. Selection uses a uniformly distributed random strategy
   *
   * @param possibilities the possible subchains
   * @return a new builder with a random switch added to its actions
   */
  def uniformRandomSwitch(possibilities: ChainBuilder*): B = {
    require(possibilities.size >= 2, "uniformRandomSwitch() requires at least 2 possibilities")

    val possibility1 :: tailPossibilities = possibilities.toList
    val basePercentage = 100d / (tailPossibilities.size + 1)
    val firstPercentage = 100d - basePercentage * tailPossibilities.size

    val possibilitiesWithPercentage = (firstPercentage, possibility1) :: tailPossibilities.map((basePercentage, _))

    randomSwitch(possibilitiesWithPercentage, None)
  }

  /**
   * Add a switch in the chain. Selection uses a round robin strategy
   *
   * @param possibilities the possible subchains
   * @return a new builder with a random switch added to its actions
   */
  def roundRobinSwitch(possibilities: ChainBuilder*): B = {
    require(possibilities.size >= 1, "roundRobinSwitch() requires at least 1 possibility")
    exec(new RoundRobinSwitchBuilder(possibilities.toList))
  }
}

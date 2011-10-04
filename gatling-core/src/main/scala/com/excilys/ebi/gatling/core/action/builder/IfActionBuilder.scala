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
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder

/**
 * Companion Object of IfActionBuilder class
 */
object IfActionBuilder {
  /**
   * Creates an initialized IfActionBuilder
   */
  def ifActionBuilder = new IfActionBuilder(None, None, None, None, Some(Nil))
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
class IfActionBuilder(val testFunction: Option[Context => Boolean], val nextTrue: Option[AbstractActionBuilder], val nextFalse: Option[AbstractActionBuilder],
                      val next: Option[Action], val groups: Option[List[String]])
    extends AbstractActionBuilder {

  /**
   * Adds testFunction to builder
   *
   * @param testFunction the test function
   * @return a new builder with testFunction set
   */
  def withTestFunction(testFunction: Context => Boolean) = new IfActionBuilder(Some(testFunction), nextTrue, nextFalse, next, groups)

  /**
   * Adds nextTrue to builder
   *
   * @param nextTrue the chain executed if testFunction evaluated to true
   * @return a new builder with nextTrue set
   */
  def withNextTrue(nextTrue: AbstractActionBuilder) = new IfActionBuilder(testFunction, Some(nextTrue), nextFalse, next, groups)

  /**
   * Adds nextFalse to builder
   *
   * @param nextFalse the chain executed if testFunction evaluated to false
   * @return a new builder with nextFalse set
   */
  def withNextFalse(nextFalse: Option[AbstractActionBuilder]) = new IfActionBuilder(testFunction, nextTrue, nextFalse, next, groups)

  def withNext(next: Action) = new IfActionBuilder(testFunction, nextTrue, nextFalse, Some(next), groups)

  def inGroups(groups: List[String]) = new IfActionBuilder(testFunction, nextTrue, nextFalse, next, Some(groups))

  def build(scenarioId: Int): Action = {
    logger.debug("Building IfAction")

    val actionTrue = nextTrue.get.withNext(next.get).inGroups(groups.get).build(scenarioId)
    val actionFalse = nextFalse.map { chain =>
      chain.withNext(next.get).inGroups(groups.get).build(scenarioId)
    }

    TypedActor.newInstance(classOf[Action], new IfAction(testFunction.get, actionTrue, actionFalse, next.get))
  }
}
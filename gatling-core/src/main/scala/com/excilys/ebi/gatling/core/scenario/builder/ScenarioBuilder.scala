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
package com.excilys.ebi.gatling.core.scenario.builder

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.EndActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.IfActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.WhileActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.GroupActionBuilder
import com.excilys.ebi.gatling.core.action.builder.GroupActionBuilder._
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.excilys.ebi.gatling.core.context.Context

/**
 * Companion object of ScenarioBuilder class
 */
object ScenarioBuilder {
  /**
   * Contains the expected durations of the scenarios
   */
  private var expectedExecutionDuration: Map[Int, Long] = Map.empty
  /**
   * Contains the current groups in the scenario, to help building the scenario
   */
  var currentGroups: List[String] = Nil

  /**
   * Adds a duration for a given scenario, this method is used to determine
   * the expected duration of scenarios
   *
   * @param scenarioId the id of the scenario (it is the same as the user Id)
   * @param durationValue the duration added
   * @param durationUnit the unit of the duration added
   * @return Nothing
   */
  def addToExecutionTime(scenarioId: Int, durationValue: Long, durationUnit: TimeUnit) = {
    expectedExecutionDuration += (scenarioId -> (TimeUnit.MILLISECONDS.convert(durationValue, durationUnit) + expectedExecutionDuration.get(scenarioId).getOrElse(0L)))
  }

  /**
   * Returns the expected execution duration for one scenario in seconds
   *
   * @param scenarioId the id of the scenario
   * @return the expected execution duration for the scenario with the id scenarioId
   */
  def getExecutionTime(scenarioId: Int) = TimeUnit.SECONDS.convert(expectedExecutionDuration.get(scenarioId).get, TimeUnit.MILLISECONDS)
}

/**
 * The scenario builder is used in the DSL to define the scenario
 *
 * It must be extended by other modules to add functionalities
 *
 * @param name the name of the scenario
 * @param actionBuilders the list of all the actions that compose the scenario
 * @param next the action that will be executed after this scenario (that can be a chain as well)
 * @param groups the groups for all the actions of this scenario
 */
abstract class ScenarioBuilder[B <: ScenarioBuilder[B]](name: String, actionBuilders: List[AbstractActionBuilder], next: Option[Action], groups: Option[List[String]])
    extends AbstractActionBuilder {

  /**
   * @return the list of the action builders
   */
  def actionsList = actionBuilders

  /**
   * @return the name of this scenario
   */
  def getName = name

  /**
   * This method creates a new instance of the class that inherits ScenarioBuilder,
   * it must be implemented in the class that inherits ScenarioBuilder
   *
   * @param name the name of the scenario
   * @param actionBuilders list of the actions composing this scenario
   * @param next the action that will be executed after this scenario (that can be a chain as well)
   * @param groups the groups for all the actions of this scenario
   * @return a new instance of the class that inherits ScenarioBuilder
   */
  def newInstance(name: String, actionBuilders: List[AbstractActionBuilder], next: Option[Action], groups: Option[List[String]]): B

  /**
   * Method used to define a pause of X seconds
   *
   * @param delayValue the time, in seconds, for which the user waits/thinks
   * @return a new builder with a pause added to its actions
   */
  def pause(delayValue: Int): B = {
    pause(delayValue, delayValue, TimeUnit.SECONDS)
  }

  /**
   * Method used to define a pause
   *
   * @param delayValue the time for which the user waits/thinks
   * @param delayUnit the time unit of the pause
   * @return a new builder with a pause added to its actions
   */
  def pause(delayValue: Int, delayUnit: TimeUnit): B = {
    pause(delayValue, delayValue, delayUnit)
  }

  /**
   * Method used to define a random pause in seconds
   *
   * @param delayMinValue the minimum value of the pause, in seconds
   * @param delayMaxValue the maximum value of the pause, in seconds
   * @return a new builder with a pause added to its actions
   */
  def pause(delayMinValue: Int, delayMaxValue: Int): B = {
    pause(delayMinValue * 1000, delayMaxValue * 1000, TimeUnit.MILLISECONDS)
  }

  /**
   * Method used to define a random pause
   *
   * @param delayMinValue the minimum value of the pause
   * @param delayMaxValue the maximum value of the pause
   * @param delayUnit the time unit of the specified values
   * @return a new builder with a pause added to its actions
   */
  def pause(delayMinValue: Int, delayMaxValue: Int, delayUnit: TimeUnit): B = {
    logger.debug("Adding PauseAction")
    newInstance(name, (pauseActionBuilder withMinDuration delayMinValue withMaxDuration delayMaxValue withTimeUnit delayUnit) :: actionBuilders, next, groups)
  }

  /**
   * Method used to add a conditional execution in the scenario
   *
   * @param testFunction the function that will determine if the condition is satisfied or not
   * @param chainTrue the chain to be executed if the condition is satisfied
   * @return a new builder with a conditional execution added to its actions
   */
  def doIf(testFunction: Context => Boolean, chainTrue: B): B = {
    doIf(testFunction, chainTrue, None)
  }

  /**
   * Method used to add a conditional execution in the scenario with a fall back
   * action if condition is not satisfied
   *
   * @param testFunction the function that will determine if the condition is satisfied or not
   * @param chainTrue the chain to be executed if the condition is satisfied
   * @param chainFalse the chain to be executed if the condition is not satisfied
   * @return a new builder with a conditional execution added to its actions
   */
  def doIf(testFunction: Context => Boolean, chainTrue: B, chainFalse: B): B = {
    doIf(testFunction, chainTrue, Some(chainFalse))
  }

  /**
   * Method used to add a conditional execution in the scenario
   *
   * @param contextKey the key of the context value to be tested for equality
   * @param value the value to which the context value must be equals
   * @param chainTrue the chain to be executed if the condition is satisfied
   * @return a new builder with a conditional execution added to its actions
   */
  def doIf(contextKey: String, value: String, chainTrue: B): B = {
    doIf((c: Context) => c.getAttribute(contextKey) == value, chainTrue)
  }

  /**
   * Method used to add a conditional execution in the scenario with a fall back
   * action if condition is not satisfied
   *
   * @param contextKey the key of the context value to be tested for equality
   * @param value the value to which the context value must be equals
   * @param chainTrue the chain to be executed if the condition is satisfied
   * @param chainFalse the chain to be executed if the condition is not satisfied
   * @return a new builder with a conditional execution added to its actions
   */
  def doIf(contextKey: String, value: String, chainTrue: B, chainFalse: B): B = {
    doIf((c: Context) => c.getAttribute(contextKey) == value, chainTrue, chainFalse)
  }

  /**
   * Private method that actually adds the If Action to the scenario
   *
   * @param testFunction the function that will determine if the condition is satisfied or not
   * @param chainTrue the chain to be executed if the condition is satisfied
   * @param chainFalse the chain to be executed if the condition is not satisfied
   * @return a new builder with a conditional execution added to its actions
   */
  private def doIf(testFunction: Context => Boolean, chainTrue: B, chainFalse: Option[B]): B = {
    logger.debug("Adding IfAction")
    newInstance(name, (ifActionBuilder withTestFunction testFunction withNextTrue chainTrue withNextFalse chainFalse inGroups groups.get) :: actionBuilders, next, groups)
  }

  /**
   * Method used to add a timed loop in the scenario, in seconds
   *
   * @param durationValue the value, in seconds, of the time that will be spent in the loop
   * @param chain the chain of actions to be executed
   * @return a new builder with a conditional loop added to its actions
   */
  def doFor(durationValue: Int, chain: B): B = {
    doFor(durationValue, TimeUnit.SECONDS, chain)
  }

  /**
   * Method used to add a timed loop in the scenario
   *
   * @param durationValue the value, in seconds, of the time that will be spent in the loop
   * @param durationUnit the time unit of the duration of the loop
   * @param chain the chain of actions to be executed
   * @return a new builder with a conditional loop added to its actions
   */
  def doFor(durationValue: Int, durationUnit: TimeUnit, chain: B): B = {
    doWhile((c: Context) => c.getWhileDuration <= durationUnit.toMillis(durationValue), chain)
  }

  /**
   * Method used to add a conditional loop to the scenario
   *
   * @param contextKey the key of the context value that will be tested for equality
   * @param value the value to which the context value will be tested
   * @param chain the chain of actions that will be executed in the loop
   * @return a new builder with a conditional loop added to its actions
   */
  def doWhile(contextKey: String, value: String, chain: B): B = {
    doWhile((c: Context) => c.getAttribute(contextKey) == value, chain)
  }

  /**
   * Method used to add a conditional loop to the scenario
   *
   * @param testFunction the function that will determine if the condition is statisfied or not
   * @param chain the chain of actions that will be executed in the loop
   * @return a new builder with a conditional loop added to its actions
   */
  def doWhile(testFunction: Context => Boolean, chain: B): B = {
    logger.debug("Adding While Action")
    newInstance(name, (whileActionBuilder withTestFunction testFunction withNextTrue chain inGroups groups.get) :: actionBuilders, next, groups)
  }

  /**
   * Method used to specify that all succeeding actions will belong to the specified group
   *
   * @param groupName the name of the group
   * @return a new builder with a group tag added to its actions
   */
  def startGroup(groupName: String): B = newInstance(name, startGroupBuilder(groupName) :: actionBuilders, next, groups)

  /**
   * Method used to specify that all succeeding actions will not belong to the specified group
   *
   * @param groupName the name of the group
   * @return a new builder with a group tag added to its actions
   */
  def endGroup(groupName: String): B = newInstance(name, endGroupBuilder(groupName) :: actionBuilders, next, groups)

  /**
   * Method used to insert an existing chain inside the current scenario
   *
   * @param chain the chain to be included in the scenario
   * @return a new builder with all actions from the chain added to its actions
   */
  def insertChain(chain: B): B = {
    newInstance(name, chain.actionsList ::: actionBuilders, next, groups)
  }

  /**
   * Method used to loop for a specified number of times. It actually unfold the for and generates
   * as much actions as needed.
   *
   * @param times the number of times that the actions must be repeated
   * @param chain the actions to be repeated
   * @return a new builder with a chain of all actions to be executed added to its actions
   */
  def iterate(times: Int, chain: B): B = {
    val chainActions: List[AbstractActionBuilder] = chain.actionsList
    var iteratedActions: List[AbstractActionBuilder] = Nil
    for (i <- 1 to times) {
      iteratedActions = chainActions ::: iteratedActions
    }
    logger.debug("Adding {} Iterations", times)
    newInstance(name, iteratedActions ::: actionBuilders, next, groups)
  }

  /**
   * Method that should not be used in a script. It adds an EndAction that will
   * tell the engine that the scenario is finished
   *
   * @param latch the countdown latch used to stop the engine
   * @return a new builder with its last action added
   */
  def end(latch: CountDownLatch): B = {
    logger.debug("Adding EndAction")
    newInstance(name, endActionBuilder(latch) :: actionBuilders, next, groups)
  }

  /**
   * Method that sets next action (used for chains)
   *
   * @param next the action to be executed after the chain
   * @return the last built action
   */
  def withNext(next: Action) = newInstance(name, actionBuilders, Some(next), groups)

  /**
   * Method that sets the group of a chain
   *
   * @param groups the list of groups in which the chain is
   * @return a new builder with its groups set
   */
  def inGroups(groups: List[String]) = newInstance(name, actionBuilders, next, Some(groups))

  /**
   * Method that actually build the scenario
   *
   * @param scenarioId the id of the current scenario
   * @return the first action of the scenario to be executed
   */
  def build(scenarioId: Int): Action = {
    var previousInList: Action = next.getOrElse(null)
    for (actionBuilder <- actionBuilders) {
      actionBuilder match {
        case b: GroupActionBuilder =>
          ScenarioBuilder.currentGroups =
            if (b.isEnd)
              ScenarioBuilder.currentGroups filterNot (_ == b.getName)
            else
              b.getName :: ScenarioBuilder.currentGroups
        case _ =>
          previousInList = actionBuilder withNext previousInList inGroups ScenarioBuilder.currentGroups build (scenarioId)
      }

    }
    previousInList
  }
}

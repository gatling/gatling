/**
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
package com.excilys.ebi.gatling.core.structure.builder
import java.util.concurrent.TimeUnit
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.IfActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.WhileActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.GroupActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.GroupActionBuilder
import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder._
import com.excilys.ebi.gatling.core.structure.loop.builder.LoopBuilder
import com.excilys.ebi.gatling.core.action.builder.CountBasedIterationActionBuilder._

abstract class AbstractStructureBuilder[B <: AbstractStructureBuilder[B]](actionBuilders: List[AbstractActionBuilder])
		extends Logging {

	private var currentGroups: List[String] = Nil

	private[builder] def setCurrentGroups(groups: List[String]) = {
		currentGroups = groups
	}

	private[structure] def getCurrentGroups = currentGroups

	def newInstance(actionBuilders: List[AbstractActionBuilder]): B

	def getActionBuilders = actionBuilders

	def exec(actionBuilder: AbstractActionBuilder): B = {
		newInstance(actionBuilder :: actionBuilders)
	}

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
		newInstance((pauseActionBuilder withMinDuration delayMinValue withMaxDuration delayMaxValue withTimeUnit delayUnit) :: actionBuilders)
	}
	/**
	 * Method used to add a conditional execution in the scenario
	 *
	 * @param testFunction the function that will determine if the condition is satisfied or not
	 * @param chainTrue the chain to be executed if the condition is satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIf(testFunction: Context => Boolean, chainTrue: ChainBuilder): B = {
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
	def doIf(testFunction: Context => Boolean, chainTrue: ChainBuilder, chainFalse: ChainBuilder): B = {
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
	def doIf(contextKey: String, value: String, chainTrue: ChainBuilder): B = {
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
	def doIf(contextKey: String, value: String, chainTrue: ChainBuilder, chainFalse: ChainBuilder): B = {
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
	private def doIf(testFunction: Context => Boolean, chainTrue: ChainBuilder, chainFalse: Option[ChainBuilder]): B = {
		logger.debug("Adding IfAction")
		newInstance((ifActionBuilder withConditionFunction testFunction withThenNext chainTrue withElseNext chainFalse inGroups getCurrentGroups) :: actionBuilders)
	}

	/**
	 * Method used to specify that all succeeding actions will belong to the specified group
	 *
	 * @param groupName the name of the group
	 * @return a new builder with a group tag added to its actions
	 */
	def startGroup(groupName: String): B = newInstance(startGroupBuilder(groupName) :: actionBuilders)

	/**
	 * Method used to specify that all succeeding actions will not belong to the specified group
	 *
	 * @param groupName the name of the group
	 * @return a new builder with a group tag added to its actions
	 */
	def endGroup(groupName: String): B = newInstance(endGroupBuilder(groupName) :: actionBuilders)
	/**
	 * Method used to insert an existing chain inside the current scenario
	 *
	 * @param chain the chain to be included in the scenario
	 * @return a new builder with all actions from the chain added to its actions
	 */
	def insertChain(chain: ChainBuilder): B = {
		newInstance(chain.getActionBuilders ::: actionBuilders)
	}

	def loop(chain: ChainBuilder) = new LoopBuilder[B](getInstance, chain, None)

	def build: Any

	protected def getInstance: B

	private[structure] def addActionBuilders(actionBuildersToAdd: List[AbstractActionBuilder]): B = {
		newInstance(actionBuildersToAdd ::: actionBuilders)
	}

	private[builder] def buildActions(initialValue: Action): Action = {
		var previousInList: Action = initialValue
		for (actionBuilder <- actionBuilders) {
			actionBuilder match {
				case group: GroupActionBuilder =>
					setCurrentGroups(
						if (group.isEnd)
							getCurrentGroups filterNot (_ == group.getName)
						else
							group.getName :: getCurrentGroups)
				case _ =>
					previousInList = actionBuilder withNext previousInList inGroups getCurrentGroups build
			}
		}
		previousInList
	}

}


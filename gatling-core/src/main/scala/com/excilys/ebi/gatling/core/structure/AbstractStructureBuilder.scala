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
package com.excilys.ebi.gatling.core.structure
import java.util.concurrent.TimeUnit
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.IfActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.WhileActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.GroupActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.GroupActionBuilder
import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder._
import com.excilys.ebi.gatling.core.structure.loop.LoopBuilder
import com.excilys.ebi.gatling.core.action.builder.CountBasedIterationActionBuilder._
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.util.StringHelper.interpolate

/**
 * This class defines most of the scenario related DSL
 *
 * @param actionBuilders the builders that represent the chain of actions of a scenario/chain
 */
abstract class AbstractStructureBuilder[B <: AbstractStructureBuilder[B]](val actionBuilders: List[AbstractActionBuilder])
		extends Logging {

	private var currentGroups: List[String] = Nil

	/**
	 * This method sets the current groups to groups
	 *
	 * @param groups the groups that are currently active
	 */
	private[core] def setCurrentGroups(groups: List[String]) = {
		currentGroups = groups
	}

	/**
	 * This method gets the current groups
	 */
	private[core] def getCurrentGroups = currentGroups

	private[core] def newInstance(actionBuilders: List[AbstractActionBuilder]): B

	/**
	 * Method used to execute an action
	 *
	 * @param actionBuilder the action builder representing the action to be executed
	 */
	def exec(actionBuilder: AbstractActionBuilder): B = newInstance(actionBuilder :: actionBuilders)

	/**
	 * Method used to define a pause of X seconds
	 *
	 * @param delayValue the time, in seconds, for which the user waits/thinks
	 * @return a new builder with a pause added to its actions
	 */
	def pause(delayValue: Int): B = pause(delayValue, delayValue, TimeUnit.SECONDS)

	/**
	 * Method used to define a pause
	 *
	 * @param delayValue the time for which the user waits/thinks
	 * @param delayUnit the time unit of the pause
	 * @return a new builder with a pause added to its actions
	 */
	def pause(delayValue: Int, delayUnit: TimeUnit): B = pause(delayValue, delayValue, delayUnit)

	/**
	 * Method used to define a random pause in seconds
	 *
	 * @param delayMinValue the minimum value of the pause, in seconds
	 * @param delayMaxValue the maximum value of the pause, in seconds
	 * @return a new builder with a pause added to its actions
	 */
	def pause(delayMinValue: Int, delayMaxValue: Int): B = pause(delayMinValue * 1000, delayMaxValue * 1000, TimeUnit.MILLISECONDS)

	/**
	 * Method used to define a random pause
	 *
	 * @param delayMinValue the minimum value of the pause
	 * @param delayMaxValue the maximum value of the pause
	 * @param delayUnit the time unit of the specified values
	 * @return a new builder with a pause added to its actions
	 */
	def pause(delayMinValue: Int, delayMaxValue: Int, delayUnit: TimeUnit): B = {
		newInstance((pauseActionBuilder withMinDuration delayMinValue withMaxDuration delayMaxValue withTimeUnit delayUnit) :: actionBuilders)
	}
	/**
	 * Method used to add a conditional execution in the scenario
	 *
	 * @param conditionFunction the function that will determine if the condition is satisfied or not
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIf(conditionFunction: Session => Boolean, thenNext: ChainBuilder): B = doIf(conditionFunction, thenNext, None)

	/**
	 * Method used to add a conditional execution in the scenario with a fall back
	 * action if condition is not satisfied
	 *
	 * @param conditionFunction the function that will determine if the condition is satisfied or not
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @param elseNext the chain to be executed if the condition is not satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIf(conditionFunction: Session => Boolean, thenNext: ChainBuilder, elseNext: ChainBuilder): B = doIf(conditionFunction, thenNext, Some(elseNext))

	/**
	 * Method used to add a conditional execution in the scenario
	 *
	 * @param sessionKey the key of the session value to be tested for equality
	 * @param value the value to which the session value must be equals
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIf(sessionKey: String, value: String, thenNext: ChainBuilder): B = doIf((s: Session) => interpolate(sessionKey)(s) == value, thenNext)

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
	def doIf(sessionKey: String, value: String, thenNext: ChainBuilder, elseNext: ChainBuilder): B = doIf((s: Session) => interpolate(sessionKey)(s) == value, thenNext, elseNext)

	/**
	 * Private method that actually adds the If Action to the scenario
	 *
	 * @param conditionFunction the function that will determine if the condition is satisfied or not
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @param elseNext the chain to be executed if the condition is not satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	private def doIf(conditionFunction: Session => Boolean, thenNext: ChainBuilder, elseNext: Option[ChainBuilder]): B = {
		newInstance((ifActionBuilder withConditionFunction conditionFunction withThenNext thenNext withElseNext elseNext inGroups getCurrentGroups) :: actionBuilders)
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
	def insertChain(chain: ChainBuilder): B = newInstance(chain.actionBuilders ::: actionBuilders)

	/**
	 * Method used to load data from a feeder in the current scenario
	 *
	 * @param feeder the feeder from which the values will be loaded
	 */
	def feed(feeder: Feeder): B = newInstance(simpleActionBuilder((s: Session) => s.setAttributes(feeder.next)) :: actionBuilders)

	/**
	 * Method used to declare a loop
	 *
	 * @param chain the chain of actions that should be repeated
	 */
	def loop(chain: ChainBuilder) = new LoopBuilder[B](getInstance, chain, None)

	private[core] def build: Any

	private[core] def getInstance: B

	private[core] def addActionBuilders(actionBuildersToAdd: List[AbstractActionBuilder]): B = newInstance(actionBuildersToAdd ::: actionBuilders)

	private[core] def buildActions(initialValue: Action): Action = {
		var previousInList: Action = initialValue
		actionBuilders.foreach { actionBuilder =>
			actionBuilder match {
				case group: GroupActionBuilder => setCurrentGroups(if (group.head) getCurrentGroups filterNot (_ == group.name) else group.name :: getCurrentGroups)
				case _ => previousInList = actionBuilder withNext previousInList inGroups getCurrentGroups build
			}
		}
		previousInList
	}
}


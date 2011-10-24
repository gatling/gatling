package com.excilys.ebi.gatling.core.scenario.builder
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

abstract class AbstractStructureBuilder[B <: AbstractStructureBuilder[B]](actionBuilders: List[AbstractActionBuilder])
		extends Logging {

	private var currentGroups: List[String] = Nil

	private[builder] def setCurrentGroups(groups: List[String]) = {
		currentGroups = groups
	}

	private[builder] def getCurrentGroups = currentGroups

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
		newInstance((ifActionBuilder withTestFunction testFunction withNextTrue chainTrue withNextFalse chainFalse inGroups getCurrentGroups) :: actionBuilders)
	}

	/**
	 * Method used to add a timed loop in the scenario, in seconds
	 *
	 * @param durationValue the value, in seconds, of the time that will be spent in the loop
	 * @param chain the chain of actions to be executed
	 * @return a new builder with a conditional loop added to its actions
	 */
	def doFor(durationValue: Int, chain: ChainBuilder): B = {
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
	def doFor(durationValue: Int, durationUnit: TimeUnit, chain: ChainBuilder): B = {
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
	def doWhile(contextKey: String, value: String, chain: ChainBuilder): B = {
		doWhile((c: Context) => c.getAttribute(contextKey) == value, chain)
	}

	/**
	 * Method used to add a conditional loop to the scenario
	 *
	 * @param testFunction the function that will determine if the condition is statisfied or not
	 * @param chain the chain of actions that will be executed in the loop
	 * @return a new builder with a conditional loop added to its actions
	 */
	def doWhile(testFunction: Context => Boolean, chain: ChainBuilder): B = {
		logger.debug("Adding While Action")
		newInstance((whileActionBuilder withTestFunction testFunction withNextTrue chain inGroups getCurrentGroups) :: actionBuilders)
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
	/**
	 * Method used to loop for a specified number of times. It actually unfold the for and generates
	 * as much actions as needed.
	 *
	 * @param times the number of times that the actions must be repeated
	 * @param chain the actions to be repeated
	 * @return a new builder with a chain of all actions to be executed added to its actions
	 */
	def iterate(times: Int, chain: ChainBuilder): B = {
		val chainActions: List[AbstractActionBuilder] = chain.getActionBuilders ::: List(simpleActionBuilder((c: Context) => c.incrementCounter))
		var iteratedActions: List[AbstractActionBuilder] = Nil
		for (i <- 1 to times) {
			iteratedActions = chainActions ::: iteratedActions
		}
		iteratedActions = simpleActionBuilder((c: Context) => c.expireCounter) :: iteratedActions
		logger.debug("Adding {} Iterations", times)
		newInstance(iteratedActions ::: actionBuilders)
	}

	def build: Any

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


/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.util.concurrent.TimeUnit

import scala.annotation.tailrec

import com.excilys.ebi.gatling.core.action.builder.ActionBuilder
import com.excilys.ebi.gatling.core.action.builder.CustomPauseActionBuilder.customPauseActionBuilder
import com.excilys.ebi.gatling.core.action.builder.ExpPauseActionBuilder.expPauseActionBuilder
import com.excilys.ebi.gatling.core.action.builder.IfActionBuilder.ifActionBuilder
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder.pauseActionBuilder
import com.excilys.ebi.gatling.core.action.builder.RandomSwitchBuilder.randomSwitchBuilder
import com.excilys.ebi.gatling.core.action.builder.RoundRobinSwitchBuilder.roundRobinSwitchBuilder
import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder.simpleActionBuilder
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.session.{ EvaluatableString, Session }
import com.excilys.ebi.gatling.core.structure.loop.LoopBuilder

import akka.actor.ActorRef
import akka.util.Duration
import akka.util.duration.longToDurationLong

/**
 * This class defines most of the scenario related DSL
 *
 * @param actionBuilders the builders that represent the chain of actions of a scenario/chain
 */
abstract class AbstractStructureBuilder[B <: AbstractStructureBuilder[B]](val actionBuilders: List[ActionBuilder]) {

	private[core] def newInstance(actionBuilders: List[ActionBuilder]): B

	/**
	 * Method used to execute an action
	 *
	 * @param actionBuilder the action builder representing the action to be executed
	 */
	def exec(actionBuilder: ActionBuilder): B = newInstance(actionBuilder :: actionBuilders)

	/**
	 * Method used to define a pause
	 *
	 * @param duration the time for which the user waits/thinks, in seconds
	 * @return a new builder with a pause added to its actions
	 */
	def pause(duration: Long): B = pause(duration seconds, None)

	/**
	 * Method used to define a pause
	 *
	 * @param duration the time for which the user waits/thinks
	 * @param durationUnit the time unit of the pause
	 * @return a new builder with a pause added to its actions
	 */
	@deprecated("""Will be remove in Gatling 1.4.0. Pass a akka.util.Duration such as "5 seconds" """)
	def pause(duration: Long, durationUnit: TimeUnit): B = pause(Duration(duration, durationUnit), None)

	/**
	 * Method used to define a random pause in seconds
	 *
	 * @param minDuration the minimum value of the pause, in seconds
	 * @param maxDuration the maximum value of the pause, in seconds
	 * @return a new builder with a pause added to its actions
	 */
	def pause(minDuration: Long, maxDuration: Long): B = pause(minDuration seconds, Some(maxDuration seconds))

	/**
	 * Method used to define a random pause in seconds
	 *
	 * @param minDuration the minimum value of the pause
	 * @param maxDuration the maximum value of the pause
	 * @param durationUnit the time unit of the pause
	 * @return a new builder with a pause added to its actions
	 */
	@deprecated("""Will be remove in Gatling 1.4.0. Pass a akka.util.Duration such as "5 seconds" """)
	def pause(minDuration: Long, maxDuration: Long, durationUnit: TimeUnit): B = pause(Duration(minDuration, durationUnit), Some(Duration(maxDuration, durationUnit)))

	/**
	 * Method used to define a random pause in seconds
	 *
	 * @param minDuration the minimum duration of the pause
	 * @param maxDuration the maximum duration of the pause
	 * @return a new builder with a pause added to its actions
	 */
	def pause(minDuration: Duration, maxDuration: Duration): B = pause(minDuration, Some(maxDuration))

	/**
	 * Method used to define a uniformly-distributed random pause
	 *
	 * @param minDuration the minimum value of the pause
	 * @param maxDuration the maximum value of the pause
	 * @return a new builder with a pause added to its actions
	 */
	def pause(minDuration: Duration, maxDuration: Option[Duration] = None): B = newInstance(pauseActionBuilder.withMinDuration(minDuration).withMaxDuration(maxDuration) :: actionBuilders)

	/**
	 * Method used to define drawn from an exponential distribution with the specified mean duration.
	 *
	 * @param meanDuration the mean duration of the pause
	 * @param durationUnit the time unit of the specified values
	 * @return a new builder with a pause added to its actions
	 */
	@deprecated("""Will be remove in Gatling 1.4.0. Pass a akka.util.Duration such as "5 seconds" """)
	def pauseExp(meanDuration: Long, durationUnit: TimeUnit = TimeUnit.SECONDS): B = pauseExp(Duration(meanDuration, durationUnit))

	/**
	 * Method used to define drawn from an exponential distribution with the specified mean duration.
	 *
	 * @param meanDuration the mean duration of the pause
	 * @return a new builder with a pause added to its actions
	 */
	def pauseExp(meanDuration: Duration): B = newInstance(expPauseActionBuilder.withMeanDuration(meanDuration) :: actionBuilders)

	/**
	 * Define a pause with a custom strategy
	 *
	 * @param delayGenerator the strategy for computing the pauses, in milliseconds
	 * @return a new builder with a pause added to its actions
	 */
	def pauseCustom(delayGenerator: () => Long): B = newInstance(customPauseActionBuilder.withDelayGenerator(delayGenerator) :: actionBuilders)

	/**
	 * Method used to add a conditional execution in the scenario
	 *
	 * @param condition the function that will determine if the condition is satisfied or not
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIf(condition: Session => Boolean, thenNext: ChainBuilder): B = doIf(condition, thenNext, None)

	/**
	 * Method used to add a conditional execution in the scenario with a fall back
	 * action if condition is not satisfied
	 *
	 * @param condition the function that will determine if the condition is satisfied or not
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @param elseNext the chain to be executed if the condition is not satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIf(condition: Session => Boolean, thenNext: ChainBuilder, elseNext: ChainBuilder): B = doIf(condition, thenNext, Some(elseNext))

	/**
	 * Method used to add a conditional execution in the scenario
	 *
	 * @param sessionKey the key of the session value to be tested for equality
	 * @param value the value to which the session value must be equals
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	def doIf(sessionKey: EvaluatableString, value: String, thenNext: ChainBuilder): B = doIf((session: Session) => sessionKey(session) == value, thenNext)

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
	def doIf(sessionKey: EvaluatableString, value: String, thenNext: ChainBuilder, elseNext: ChainBuilder): B = doIf((session: Session) => sessionKey(session) == value, thenNext, elseNext)

	/**
	 * Private method that actually adds the If Action to the scenario
	 *
	 * @param condition the function that will determine if the condition is satisfied or not
	 * @param thenNext the chain to be executed if the condition is satisfied
	 * @param elseNext the chain to be executed if the condition is not satisfied
	 * @return a new builder with a conditional execution added to its actions
	 */
	private def doIf(condition: Session => Boolean, thenNext: ChainBuilder, elseNext: Option[ChainBuilder]): B = newInstance(ifActionBuilder.withCondition(condition).withThenNext(thenNext).withElseNext(elseNext) :: actionBuilders)

	/**
	 * Add a switch in the chain. Every possible subchain is defined with a percentage.
	 * Switch is selected randomly. If no switch is selected (ie random number exceeds percentages sum), switch is bypassed.
	 * Percentages sum can't exceed 100%.
	 *
	 * @param possibilities the possible subchains
	 * @return a new builder with a random switch added to its actions
	 */
	def randomSwitch(possibility: (Int, ChainBuilder), possibilities: (Int, ChainBuilder)*): B = newInstance(randomSwitchBuilder.withPossibilities(possibility :: possibilities.toList) :: actionBuilders)

	/**
	 * Add a switch in the chain. Selection uses a round robin strategy
	 *
	 * @param possibilities the possible subchains
	 * @return a new builder with a random switch added to its actions
	 */
	def roundRobinSwitch(possibility1: ChainBuilder, possibility2: ChainBuilder, possibilities: ChainBuilder*): B = newInstance(roundRobinSwitchBuilder.withPossibilities(possibility1 :: possibility2 :: possibilities.toList) :: actionBuilders)

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
	def feed(feeder: Feeder): B = newInstance(simpleActionBuilder((session: Session) => session.setAttributes(feeder.next)) :: actionBuilders)

	/**
	 * Method used to declare a loop
	 *
	 * @param chain the chain of actions that should be repeated
	 */
	def loop(chain: ChainBuilder) = new LoopBuilder[B](getInstance, chain, None)

	private[core] def getInstance: B

	private[core] def addActionBuilders(actionBuildersToAdd: List[ActionBuilder]): B = newInstance(actionBuildersToAdd ::: actionBuilders)

	protected def buildChainedActions(initialValue: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry): ActorRef = {

		@tailrec
		def buildChainedActions(actorRef: ActorRef, actionBuilders: List[ActionBuilder], protocolConfigurationRegistry: ProtocolConfigurationRegistry): ActorRef = {
			actionBuilders match {
				case Nil => actorRef
				case firstBuilder :: restOfBuilders => buildChainedActions(firstBuilder.withNext(actorRef).build(protocolConfigurationRegistry), restOfBuilders, protocolConfigurationRegistry)
			}
		}

		buildChainedActions(initialValue, actionBuilders, protocolConfigurationRegistry)
	}
}


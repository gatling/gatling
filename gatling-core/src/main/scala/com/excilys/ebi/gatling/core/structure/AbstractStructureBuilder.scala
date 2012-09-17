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

import java.util.UUID
import java.util.concurrent.TimeUnit

import scala.annotation.tailrec

import com.excilys.ebi.gatling.core.action.builder.{ ActionBuilder, BypassSimpleActionBuilder, CustomPauseActionBuilder, ExpPauseActionBuilder, IfActionBuilder, PauseActionBuilder, RandomSwitchBuilder, RoundRobinSwitchBuilder, SimpleActionBuilder }
import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.session.{ EvaluatableString, Session }
import com.excilys.ebi.gatling.core.structure.loop.LoopBuilder
import com.excilys.ebi.gatling.core.structure.loop.handler.{ ConditionalLoopHandlerBuilder, DurationLoopHandlerBuilder, TimesLoopHandlerBuilder, TryMaxLoopHandlerBuilder }
import com.excilys.ebi.gatling.core.util.StringHelper.parseEvaluatable

import akka.actor.ActorRef
import akka.util.Duration
import akka.util.duration.longToDurationLong
import grizzled.slf4j.Logging

/**
 * This class defines most of the scenario related DSL
 *
 * @param actionBuilders the builders that represent the chain of actions of a scenario/chain
 */
abstract class AbstractStructureBuilder[B <: AbstractStructureBuilder[B]] extends Logging {

	private[core] def actionBuilders: List[ActionBuilder]

	private[core] def newInstance(actionBuilders: List[ActionBuilder]): B

	/**
	 * Method used to execute an action
	 *
	 * @param actionBuilder the action builder representing the action to be executed
	 */
	def exec(sessionFunction: Session => Session): B = exec(BypassSimpleActionBuilder(sessionFunction))
	def exec(actionBuilder: ActionBuilder): B = newInstance(actionBuilder :: actionBuilders)
	def exec(chains: ChainBuilder*): B = exec(chains.toIterable)
	def exec(chains: Iterator[ChainBuilder]): B = exec(chains.toIterable)
	def exec(chains: Iterable[ChainBuilder]): B = newInstance(chains.toList.reverse.map(_.actionBuilders).flatten ::: actionBuilders)

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
	@deprecated("""Will be remove in Gatling 1.4.0. Pass a akka.util.Duration such as "5 seconds"""", "1.3.0")
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
	@deprecated("""Will be remove in Gatling 1.4.0. Pass a akka.util.Duration such as "5 seconds"""", "1.3.0")
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
	def pause(minDuration: Duration, maxDuration: Option[Duration] = None): B = newInstance(PauseActionBuilder(minDuration, maxDuration) :: actionBuilders)

	/**
	 * Method used to define drawn from an exponential distribution with the specified mean duration.
	 *
	 * @param meanDuration the mean duration of the pause
	 * @param durationUnit the time unit of the specified values
	 * @return a new builder with a pause added to its actions
	 */
	@deprecated("""Will be remove in Gatling 1.4.0. Pass a akka.util.Duration such as "5 seconds"""", "1.3.0")
	def pauseExp(meanDuration: Long, durationUnit: TimeUnit): B = pauseExp(Duration(meanDuration, durationUnit))

	/**
	 * Method used to define drawn from an exponential distribution with the specified mean duration.
	 *
	 * @param meanDuration the mean duration of the pause, in seconds
	 * @return a new builder with a pause added to its actions
	 */
	def pauseExp(meanDuration: Long): B = newInstance(ExpPauseActionBuilder(meanDuration seconds) :: actionBuilders)

	/**
	 * Method used to define drawn from an exponential distribution with the specified mean duration.
	 *
	 * @param meanDuration the mean duration of the pause
	 * @return a new builder with a pause added to its actions
	 */
	def pauseExp(meanDuration: Duration): B = newInstance(ExpPauseActionBuilder(meanDuration) :: actionBuilders)

	/**
	 * Define a pause with a custom strategy
	 *
	 * @param delayGenerator the strategy for computing the pauses, in milliseconds
	 * @return a new builder with a pause added to its actions
	 */
	def pauseCustom(delayGenerator: () => Long): B = newInstance(CustomPauseActionBuilder(delayGenerator) :: actionBuilders)

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
	private def doIf(condition: Session => Boolean, thenNext: ChainBuilder, elseNext: Option[ChainBuilder]): B = newInstance(IfActionBuilder(condition = condition, thenNext = thenNext, elseNext = elseNext) :: actionBuilders)

	/**
	 * Add a switch in the chain. Every possible subchain is defined with a percentage.
	 * Switch is selected randomly. If no switch is selected (ie random number exceeds percentages sum), switch is bypassed.
	 * Percentages sum can't exceed 100%.
	 *
	 * @param possibility1 the first possible subchain
	 * @param possibility2 the second possible subchain
	 * @param possibilities the rest of the possible subchains
	 * @return a new builder with a random switch added to its actions
	 */
	def randomSwitch(possibility1: (Int, ChainBuilder), possibility2: (Int, ChainBuilder), possibilities: (Int, ChainBuilder)*): B = newInstance(RandomSwitchBuilder(possibility1 :: possibility2 :: possibilities.toList) :: actionBuilders)

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
		val basePercentage = 100 / (tailPossibilities.size + 1)
		val firstPercentage = 100 - basePercentage * tailPossibilities.size

		val possibilitiesWithPercentage = (firstPercentage, possibility1) :: tailPossibilities.map((basePercentage, _))

		newInstance(RandomSwitchBuilder(possibilitiesWithPercentage) :: actionBuilders)
	}

	/**
	 * Add a switch in the chain. Selection uses a round robin strategy
	 *
	 * @param possibility1 the first possible subchain
	 * @param possibility2 the second possible subchain
	 * @param possibilities the rest of the possible subchains
	 * @return a new builder with a random switch added to its actions
	 */
	def roundRobinSwitch(possibility1: ChainBuilder, possibility2: ChainBuilder, possibilities: ChainBuilder*): B = newInstance(RoundRobinSwitchBuilder(possibility1 :: possibility2 :: possibilities.toList) :: actionBuilders)

	/**
	 * Method used to insert an existing chain inside the current scenario
	 *
	 * @param chain the chain to be included in the scenario
	 * @return a new builder with all actions from the chain added to its actions
	 */
	@deprecated("""Will be removed in Gatling 1.4.0. Use "exec" instead.""", "1.3.0")
	def insertChain(chain: ChainBuilder): B = newInstance(chain.actionBuilders ::: actionBuilders)

	/**
	 * Method used to load data from a feeder in the current scenario
	 *
	 * @param feeder the feeder from which the values will be loaded
	 */
	def feed(feeder: Feeder): B = {

		val feedFunction = (session: Session) => {
			if (!feeder.hasNext) {
				error("Feeder is now empty, stopping engine")
				system.shutdown
				sys.exit
			}

			session.setAttributes(feeder.next)
		}
		newInstance(BypassSimpleActionBuilder(feedFunction) :: actionBuilders)
	}

	/**
	 * Method used to declare a loop
	 *
	 * @param chain the chain of actions that should be repeated
	 */
	@deprecated("Will be removed in Gatling 1.4.0.", "1.3.0")
	def loop(chain: ChainBuilder) = new LoopBuilder[B](getInstance, chain, None)

	def repeat(times: Int)(chain: ChainBuilder): B = repeat(times, None, chain)
	def repeat(times: Int, counterName: String)(chain: ChainBuilder): B = repeat(times, Some(counterName), chain)
	private def repeat(times: Int, counterName: Option[String], chain: ChainBuilder): B = new TimesLoopHandlerBuilder(getInstance, chain, times, counterName).build

	def repeat(times: String)(chain: ChainBuilder): B = repeat(times, None, chain)
	def repeat(times: String, counterName: String)(chain: ChainBuilder): B = repeat(times, Some(counterName), chain)
	private def repeat(times: String, counterName: Option[String], chain: ChainBuilder): B = {
		val sessionFunction = parseEvaluatable(times)
		repeat((s: Session) => sessionFunction(s).toInt, counterName, chain)
	}

	def repeat(times: Session => Int)(chain: ChainBuilder): B = repeat(times, None, chain)
	def repeat(times: Session => Int, counterName: String)(chain: ChainBuilder): B = repeat(times, Some(counterName), chain)
	private def repeat(times: Session => Int, counterName: Option[String] = None, chain: ChainBuilder): B = {
		val counter = counterName.getOrElse(UUID.randomUUID.toString)
		asLongAs((s: Session) => s.getCounterValue(counter) < times(s), Some(counter), chain)
	}

	def during(duration: Long)(chain: ChainBuilder): B = during(duration seconds, None, chain)
	def during(duration: Long, counterName: String)(chain: ChainBuilder): B = during(duration seconds, Some(counterName), chain)
	def during(duration: Duration)(chain: ChainBuilder): B = during(duration, None, chain)
	def during(duration: Duration, counterName: String)(chain: ChainBuilder): B = during(duration, Some(counterName), chain)
	private def during(duration: Duration, counterName: Option[String], chain: ChainBuilder): B = new DurationLoopHandlerBuilder(getInstance, chain, duration, counterName).build

	def asLongAs(condition: Session => Boolean)(chain: ChainBuilder): B = asLongAs(condition, None, chain)
	def asLongAs(condition: Session => Boolean, counterName: String)(chain: ChainBuilder): B = asLongAs(condition, Some(counterName), chain)
	private def asLongAs(condition: Session => Boolean, counterName: Option[String], chain: ChainBuilder): B = new ConditionalLoopHandlerBuilder(getInstance, chain, condition, counterName).build

	def exitBlockOnFail(chain: ChainBuilder): B = tryMax(1)(chain)
	def tryMax(times: Int)(chain: ChainBuilder): B = tryMax(times, None)(chain)
	def tryMax(times: Int, counterName: String)(chain: ChainBuilder): B = tryMax(times, Some(counterName))(chain)
	private def tryMax(times: Int, counterName: Option[String])(chain: ChainBuilder): B = {

		def buildTransactionalChain(chain: ChainBuilder): ChainBuilder = {
			val startBlock = SimpleActionBuilder((session: Session) => session.clearFailed.setMustExitOnFail)
			val endBlock = SimpleActionBuilder((session: Session) => session.clearMustExitOnFail)
			ChainBuilder.emptyChain.exec(startBlock).exec(chain).exec(endBlock)
		}

		times match {
			case times if times >= 1 => new TryMaxLoopHandlerBuilder(getInstance, buildTransactionalChain(chain), times, counterName).build
			case times => throw new IllegalArgumentException("Can't set up a max try <= 1")
		}
	}

	def exitHereIfFailed: B = exec(SimpleActionBuilder((session: Session) => session.setMustExitOnFail))

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


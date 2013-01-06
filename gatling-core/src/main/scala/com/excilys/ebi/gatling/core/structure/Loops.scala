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

import scala.concurrent.duration.{ Duration, DurationLong }

import com.excilys.ebi.gatling.core.action.builder.{ SimpleActionBuilder, WhileActionBuilder }
import com.excilys.ebi.gatling.core.session.{ Expression, Session }
import com.excilys.ebi.gatling.core.session.handler.{ CounterBasedIterationHandler, TimerBasedIterationHandler }
import com.excilys.ebi.gatling.core.structure.ChainBuilder.emptyChain
import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis

import grizzled.slf4j.Logging

trait Loops[B] extends Execs[B] with Logging {

	def repeat(times: Int)(chain: ChainBuilder): B = repeat(times, None, chain)
	def repeat(times: Int, counterName: String)(chain: ChainBuilder): B = repeat(times, Some(counterName), chain)

	private def repeat(times: Int, counterName: Option[String], chain: ChainBuilder): B = {

		val computedCounterName = counterName.getOrElse(UUID.randomUUID.toString)

		val handler = new CounterBasedIterationHandler {
			def counterName = computedCounterName
		}

		val initAction = emptyChain.exec(SimpleActionBuilder(handler.init))
		val incrementAction = emptyChain.exec(SimpleActionBuilder(handler.increment))
		val expireAction = emptyChain.exec(SimpleActionBuilder(handler.expire))
		val innerActions = (1 to times).flatMap(_ => List(incrementAction, chain)).toList
		val allActions = initAction :: innerActions ::: List(expireAction)

		exec(allActions)
	}

	def repeat(times: String)(chain: ChainBuilder): B = repeat(times, None, chain)
	def repeat(times: String, counterName: String)(chain: ChainBuilder): B = repeat(times, Some(counterName), chain)

	private def repeat(times: String, counterName: Option[String], chain: ChainBuilder): B = {
		val timesFunction = Expression[Int](times)
		repeat(timesFunction, counterName, chain)
	}

	def repeat(times: Expression[Int])(chain: ChainBuilder): B = repeat(times, None, chain)
	def repeat(times: Expression[Int], counterName: String)(chain: ChainBuilder): B = repeat(times, Some(counterName), chain)

	private def repeat(times: Expression[Int], counterName: Option[String] = None, chain: ChainBuilder): B = {

		val counter = counterName.getOrElse(UUID.randomUUID.toString)

		def continueCondition(session: Session) = {
			for {
				counterValue <- session.safeGetAs[Int](counter)
				timesValue <- times(session)
			} yield counterValue < timesValue
		}

		asLongAs(continueCondition, Some(counter), chain)
	}

	def during(duration: Long)(chain: ChainBuilder): B = during(duration seconds, None, chain)
	def during(duration: Long, counterName: String)(chain: ChainBuilder): B = during(duration seconds, Some(counterName), chain)
	def during(duration: Duration)(chain: ChainBuilder): B = during(duration, None, chain)
	def during(duration: Duration, counterName: String)(chain: ChainBuilder): B = during(duration, Some(counterName), chain)

	private def during(duration: Duration, counterName: Option[String], chain: ChainBuilder): B = {
		val loopCounterName = counterName.getOrElse(UUID.randomUUID.toString)
		def continueCondition(session: Session) = TimerBasedIterationHandler.getTimer(session, loopCounterName)
			.map(timerStartMillis => (nowMillis - timerStartMillis) <= duration.toMillis)

		exec(WhileActionBuilder(continueCondition, chain, loopCounterName))
	}

	def asLongAs(condition: Expression[Boolean])(chain: ChainBuilder): B = asLongAs(condition, None, chain)
	def asLongAs(condition: Expression[Boolean], counterName: String)(chain: ChainBuilder): B = asLongAs(condition, Some(counterName), chain)

	private def asLongAs(condition: Expression[Boolean], counterName: Option[String], chain: ChainBuilder): B = {
		val loopCounterName = counterName.getOrElse(UUID.randomUUID.toString)
		exec(WhileActionBuilder(condition, chain, loopCounterName))
	}
}
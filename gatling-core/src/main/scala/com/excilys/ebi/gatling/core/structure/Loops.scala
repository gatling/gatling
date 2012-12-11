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

import com.excilys.ebi.gatling.core.action.builder.{ SimpleActionBuilder, WhileActionBuilder }
import com.excilys.ebi.gatling.core.session.ELParser.parseEL
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.session.handler.{ CounterBasedIterationHandler, TimerBasedIterationHandler }
import com.excilys.ebi.gatling.core.structure.ChainBuilder.emptyChain
import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis

import akka.util.Duration
import akka.util.duration.longToDurationLong

trait Loops[B] extends Execs[B] {

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
		val timesFunction = parseEL(times) andThen (_.toInt)
		repeat(timesFunction, counterName, chain)
	}

	def repeat(times: Session => Int)(chain: ChainBuilder): B = repeat(times, None, chain)
	def repeat(times: Session => Int, counterName: String)(chain: ChainBuilder): B = repeat(times, Some(counterName), chain)
	private def repeat(times: Session => Int, counterName: Option[String] = None, chain: ChainBuilder): B = {
		val counter = counterName.getOrElse(UUID.randomUUID.toString)
		def condition(session: Session) = session.getTypedAttribute[Int](counter) < times(session)
		asLongAs(condition, Some(counter), chain)
	}

	def during(duration: Long)(chain: ChainBuilder): B = during(duration seconds, None, chain)
	def during(duration: Long, counterName: String)(chain: ChainBuilder): B = during(duration seconds, Some(counterName), chain)
	def during(duration: Duration)(chain: ChainBuilder): B = during(duration, None, chain)
	def during(duration: Duration, counterName: String)(chain: ChainBuilder): B = during(duration, Some(counterName), chain)
	private def during(duration: Duration, counterName: Option[String], chain: ChainBuilder): B = {
		val loopCounterName = counterName.getOrElse(UUID.randomUUID.toString)
		def condition(session: Session) = (nowMillis - TimerBasedIterationHandler.getTimer(session, loopCounterName)) <= duration.toMillis
		exec(WhileActionBuilder(condition, chain, loopCounterName))
	}

	def asLongAs(condition: Session => Boolean)(chain: ChainBuilder): B = asLongAs(condition, None, chain)
	def asLongAs(condition: Session => Boolean, counterName: String)(chain: ChainBuilder): B = asLongAs(condition, Some(counterName), chain)
	private def asLongAs(condition: Session => Boolean, counterName: Option[String], chain: ChainBuilder): B = {
		val loopCounterName = counterName.getOrElse(UUID.randomUUID.toString)
		exec(WhileActionBuilder(condition, chain, loopCounterName))
	}
}
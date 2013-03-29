/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.util.UUID

import scala.collection.immutable.Stream
import scala.concurrent.duration.Duration

import io.gatling.core.action.builder.{ SessionHookBuilder, WhileBuilder }
import io.gatling.core.session.{ EL, Expression, Session }
import io.gatling.core.session.handler.{ CounterBasedIterationHandler, TimerBasedIterationHandler }
import io.gatling.core.structure.ChainBuilder.chainOf
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Failure, Success }

import grizzled.slf4j.Logging

trait Loops[B] extends Execs[B] with Logging {

	def repeat(times: Int)(chain: ChainBuilder): B = repeat(times, None, chain)
	def repeat(times: Int, counterName: String)(chain: ChainBuilder): B = repeat(times, Some(counterName), chain)

	private def repeat(times: Int, loopCounterName: Option[String], chain: ChainBuilder): B = {

		val handler = new CounterBasedIterationHandler {
			val counterName = loopCounterName.getOrElse(UUID.randomUUID.toString)
		}

		val init = new SessionHookBuilder(handler.init)
		val expire = new SessionHookBuilder(handler.expire)
		val increment = chainOf(new SessionHookBuilder(handler.increment))
		val flattenLoopContent = Stream.continually(List(increment, chain)).take(times).flatten

		exec(chainOf(init).exec(flattenLoopContent).exec(expire))
	}

	def repeat(times: String)(chain: ChainBuilder): B = repeat(times, None, chain)
	def repeat(times: String, counterName: String)(chain: ChainBuilder): B = repeat(times, Some(counterName), chain)

	private def repeat(times: String, counterName: Option[String], chain: ChainBuilder): B = {
		val timesFunction = EL.compile[Int](times)
		repeat(timesFunction, counterName, chain)
	}

	def repeat(times: Expression[Int])(chain: ChainBuilder): B = repeat(times, None, chain)
	def repeat(times: Expression[Int], counterName: String)(chain: ChainBuilder): B = repeat(times, Some(counterName), chain)

	private def repeat(times: Expression[Int], counterName: Option[String] = None, chain: ChainBuilder): B = {

		val counter = counterName.getOrElse(UUID.randomUUID.toString)

		def continueCondition(session: Session) = {
			for {
				counterValue <- session.safeGet[Int](counter)
				timesValue <- times(session)
			} yield counterValue < timesValue
		}

		asLongAs(continueCondition, Some(counter), chain)
	}

	def during(duration: Duration)(chain: ChainBuilder): B = during(duration, None, chain)
	def during(duration: Duration, counterName: String)(chain: ChainBuilder): B = during(duration, Some(counterName), chain)

	private def during(duration: Duration, counterName: Option[String], chain: ChainBuilder): B = {
		val loopCounterName = counterName.getOrElse(UUID.randomUUID.toString)
		def continueCondition(session: Session) = TimerBasedIterationHandler.getTimer(session, loopCounterName)
			.map(timerStartMillis => (nowMillis - timerStartMillis) <= duration.toMillis)

		exec(new WhileBuilder(continueCondition, chain, loopCounterName))
	}

	def asLongAs(condition: Expression[Boolean])(chain: ChainBuilder): B = asLongAs(condition, None, chain)
	def asLongAs(condition: Expression[Boolean], counterName: String)(chain: ChainBuilder): B = asLongAs(condition, Some(counterName), chain)

	private def asLongAs(condition: Expression[Boolean], counterName: Option[String], chain: ChainBuilder): B = {
		val loopCounterName = counterName.getOrElse(UUID.randomUUID.toString)
		exec(new WhileBuilder(condition, chain, loopCounterName))
	}

	def foreach(seq: Expression[Seq[Any]], attributeName: String, counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {
		val setNextValueInSession = new SessionHookBuilder(session => {
			val nextValue = for {
				counterValue <- session.safeGet[Int](counterName)
				seq <- seq(session)
			} yield seq(counterValue)

			nextValue match {
				case Success(value) => session.set(attributeName, value)
				case Failure(message) => error(s"Could not set attribute in foreach: $message"); throw new IllegalAccessError(message)
			}
		})

		val continueCondition = (session: Session) =>
			for {
				counterValue <- session.safeGet[Int](counterName)
				seq <- seq(session)
			} yield seq.isDefinedAt(counterValue)

		asLongAs(continueCondition, Some(counterName), chainOf(setNextValueInSession).exec(chain))
	}
}
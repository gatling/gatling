/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.structure.ChainBuilder.chainOf
import io.gatling.core.util.TimeHelper.{ nanosSinceReference, nowMillis }
import io.gatling.core.validation.SuccessWrapper

trait Loops[B] extends Execs[B] {

	def repeat(times: Expression[Int], counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {

		val continueCondition = (session: Session) => times(session).map(session.loopCounterValue(counterName) < _)

		asLongAs(continueCondition, counterName, false)(chain)
	}

	def foreach(seq: Expression[Seq[Any]], attributeName: String, counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {

		val exposeCurrentValue = (session: Session) => seq(session).map(seq => session.set(attributeName, seq(session.loopCounterValue(counterName))))
		val continueCondition = (session: Session) => seq(session).map(_.isDefinedAt(session.loopCounterValue(counterName)))

		asLongAs(continueCondition, counterName, false)(chainOf(new SessionHookBuilder(exposeCurrentValue)).exec(chain))
	}

	def during(duration: Duration, counterName: String = UUID.randomUUID.toString, exitASAP: Boolean = true)(chain: ChainBuilder): B = {

		val durationMillis = duration.toMillis
		val continueCondition = (session: Session) => (nowMillis - session.loopTimestampValue(counterName) <= durationMillis).success

		asLongAs(continueCondition, counterName, exitASAP)(chain)
	}

	def maxSimulationDuration(duration: Duration, counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {

		val durationNanos = duration.toNanos
		val condition = (session: Session) => (nanosSinceReference <= durationNanos && session.loopCounterValue(counterName) == 0).success

		asLongAs(condition, counterName)(chain)
	}

	def asLongAs(condition: Expression[Boolean], counterName: String = UUID.randomUUID.toString, exitASAP: Boolean = true)(chain: ChainBuilder): B =
		exec(new WhileBuilder(condition, chain, counterName, exitASAP))
}
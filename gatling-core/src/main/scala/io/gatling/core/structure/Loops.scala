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
import io.gatling.core.session.{ EL, Expression, Session }
import io.gatling.core.session.handler.Loop
import io.gatling.core.structure.ChainBuilder.chainOf
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Failure, Success, SuccessWrapper }
import io.gatling.core.session.SessionPrivateAttributes

trait Loops[B] extends Execs[B] {

	def repeat(times: Int)(chain: ChainBuilder): B = repeat(times, UUID.randomUUID.toString)(chain)
	def repeat(times: Int, counter: String)(chain: ChainBuilder): B = {

		val loopHandler = new Loop {
			val counterName = counter
		}

		val increment = chainOf(new SessionHookBuilder(s => loopHandler.incrementLoop(s).success))
		val exit = chainOf(new SessionHookBuilder(s => loopHandler.exitLoop(s).success))
		val reversedLoopContent = exit :: Stream.continually(List(chain, increment)).take(times).flatten.toList

		exec(reversedLoopContent.reverse)
	}

	def repeat(times: Expression[Int], counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {

		val continueCondition = (session: Session) =>
			for {
				counterValue <- session.getV[Int](counterName)
				timesValue <- times(session)
			} yield counterValue < timesValue

		exec(new WhileBuilder(continueCondition, chain, counterName, false))
	}

	def foreach(seq: Expression[Seq[Any]], attributeName: String, counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {

		val exposeCurrentValue = new SessionHookBuilder(session => {
			for {
				counterValue <- session.getV[Int](counterName)
				seq <- seq(session)
			} yield session.set(attributeName, seq(counterValue))
		})

		val continueCondition = (session: Session) =>
			for {
				counterValue <- session.getV[Int](counterName)
				seq <- seq(session)
			} yield seq.isDefinedAt(counterValue)

		exec(new WhileBuilder(continueCondition, chainOf(exposeCurrentValue).exec(chain), counterName, false))
	}

	def during(duration: Duration, counterNameParam: String = UUID.randomUUID.toString, exitASAP: Boolean = true)(chain: ChainBuilder): B = {

		val durationMillis = duration.toMillis

		val loop = new Loop {
			val counterName = counterNameParam
		}

		val continueCondition = (session: Session) => {
			val timestamp = loop.timestampValue(session)
			(nowMillis - timestamp <= durationMillis).success
		}

		asLongAs(continueCondition, exitASAP, counterNameParam)(chain)
	}

	def asLongAs(condition: Expression[Boolean], exitASAP: Boolean = true, counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B =
		exec(new WhileBuilder(condition, chain, counterName, exitASAP))
}
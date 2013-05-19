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
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.SuccessWrapper

object Loops {

	case class CounterName(name: String) extends AnyVal

	implicit class SessionCounters(val session: Session) extends AnyVal {

		def timestampName(implicit counterName: CounterName) = "timestamp." + counterName.name

		def isSetUp(implicit counterName: CounterName) = session.contains(counterName.name)

		def counterValue(implicit counterName: CounterName) = session(counterName.name).asInstanceOf[Int]
		def timestampValue(implicit counterName: CounterName) = session(timestampName).asInstanceOf[Long]

		def incrementLoop(implicit counterName: CounterName): Session = {
			if (isSetUp)
				session.set(counterName.name, counterValue + 1)
			else
				session.setAll(counterName.name -> 0, timestampName -> nowMillis)
		}

		def exitLoop(implicit counterName: CounterName): Session = session.removeAll(counterName.name, timestampName)
	}
}

trait Loops[B] extends Execs[B] {

	import Loops._

	def repeat(times: Int)(chain: ChainBuilder): B = repeat(times, UUID.randomUUID.toString)(chain)
	def repeat(times: Int, counter: String)(chain: ChainBuilder): B = {

		implicit val counterName = CounterName(counter)

		val increment = chainOf(new SessionHookBuilder(_.incrementLoop.success))
		val exit = chainOf(new SessionHookBuilder(_.exitLoop.success))
		val reversedLoopContent = exit :: Stream.continually(List(chain, increment)).take(times).flatten.toList

		exec(reversedLoopContent.reverse)
	}

	def repeat(times: Expression[Int], counter: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {

		implicit val counterName = CounterName(counter)

		val continueCondition = (session: Session) => times(session).map(session.counterValue < _)

		exec(new WhileBuilder(continueCondition, chain, false))
	}

	def foreach(seq: Expression[Seq[Any]], attributeName: String, counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B = {

		implicit val counter = CounterName(counterName)

		val exposeCurrentValue = (session: Session) => seq(session).map(seq => session.set(attributeName, seq(session.counterValue)))

		val continueCondition = (session: Session) => seq(session).map(_.isDefinedAt(session.counterValue))

		exec(new WhileBuilder(continueCondition, chainOf(new SessionHookBuilder(exposeCurrentValue)).exec(chain), false))
	}

	def during(duration: Duration, counterName: String = UUID.randomUUID.toString, exitASAP: Boolean = true)(chain: ChainBuilder): B = {

		implicit val counter = CounterName(counterName)

		val durationMillis = duration.toMillis

		val continueCondition = (session: Session) => (nowMillis - session.timestampValue <= durationMillis).success

		asLongAs(continueCondition, exitASAP, counterName)(chain)
	}

	def asLongAs(condition: Expression[Boolean], exitASAP: Boolean = true, counterName: String = UUID.randomUUID.toString)(chain: ChainBuilder): B =
		exec(new WhileBuilder(condition, chain, exitASAP)(CounterName(counterName)))
}
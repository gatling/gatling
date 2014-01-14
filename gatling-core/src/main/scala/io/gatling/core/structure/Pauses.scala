/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.{ Duration, DurationLong }
import scala.concurrent.forkjoin.ThreadLocalRandom

import io.gatling.core.action.builder.PauseBuilder
import io.gatling.core.session.{ Expression, ExpressionWrapper, Session }
import io.gatling.core.session.el.EL
import io.gatling.core.validation.SuccessWrapper

trait Pauses[B] extends Execs[B] {

	/**
	 * Method used to define a pause based on a duration defined in the session
	 *
	 * @param duration Expression that when resolved, provides the pause duration
	 * @return a new builder with a pause added to its actions
	 */
	def pause(duration: Duration): B = pause(duration.expression)
	def pause(duration: String, unit: TimeUnit = TimeUnit.SECONDS): B = {
		val durationValue = duration.el[Int]
		pause(durationValue(_).map(i => Duration(i, unit)))
	}

	def pause(min: Duration, max: Duration): B = {
		val minMillis = min.toMillis
		val maxMillis = max.toMillis

		val expression = (session: Session) => (ThreadLocalRandom.current.nextLong(minMillis, maxMillis) millis).success

		pause(expression)
	}
	def pause(min: String, max: String, unit: TimeUnit): B = {
		val minExpression = min.el[Int]
		val maxExpression = max.el[Int]

		val expression = (session: Session) =>
			for {
				min <- minExpression(session)
				max <- maxExpression(session)
				minMillis = Duration(min, unit).toMillis
				maxMillis = Duration(max, unit).toMillis

			} yield (ThreadLocalRandom.current.nextLong(minMillis, maxMillis) millis)

		pause(expression)
	}
	def pause(min: Expression[Duration], max: Expression[Duration]): B = {

		val expression = (session: Session) =>
			for {
				min <- min(session)
				max <- max(session)
				minMillis = min.toMillis
				maxMillis = max.toMillis
			} yield (ThreadLocalRandom.current.nextLong(minMillis, maxMillis) millis)

		pause(expression)
	}

	def pause(duration: Expression[Duration]): B = exec(new PauseBuilder(duration))
}

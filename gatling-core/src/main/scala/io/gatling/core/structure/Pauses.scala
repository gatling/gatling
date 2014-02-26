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

import scala.concurrent.duration.{ FiniteDuration, Duration, DurationLong }
import scala.concurrent.forkjoin.ThreadLocalRandom

import io.gatling.core.action.builder.{ PaceBuilder, PauseBuilder, RendezVousBuilder }
import io.gatling.core.session.{ Expression, ExpressionWrapper, Session }
import io.gatling.core.session.el.EL
import io.gatling.core.validation.{ Validation, SuccessWrapper }
import java.util.UUID

trait Pauses[B] extends Execs[B] {

	/**
	 * Method used to define a pause based on a duration defined in the session
	 *
	 * @param duration Expression that when resolved, provides the pause duration
	 * @return a new builder with a pause added to its actions
	 */
	def pause(duration: Duration): B = pause(duration.expression)

	private def durationExpression(duration: String, unit: TimeUnit): Expression[Duration] = {
		val durationValue = duration.el[Int]
		durationValue(_).map(i => Duration(i, unit))
	}

	private def durationExpression(min: Duration, max: Duration)(session: Session): Validation[Duration] = {
		val minMillis = min.toMillis
		val maxMillis = max.toMillis

		(ThreadLocalRandom.current.nextLong(minMillis, maxMillis) millis).success
	}

	private def durationExpression(min: String, max: String, unit: TimeUnit)(session: Session): Validation[Duration] = {
		val minExpression = min.el[Int]
		val maxExpression = max.el[Int]

		for {
			min <- minExpression(session)
			max <- maxExpression(session)
			minMillis = Duration(min, unit).toMillis
			maxMillis = Duration(max, unit).toMillis
		} yield ThreadLocalRandom.current.nextLong(minMillis, maxMillis) millis
	}

	private def durationExpression(min: Expression[Duration], max: Expression[Duration])(session: Session): Validation[Duration] = {
		for {
			min <- min(session)
			max <- max(session)
			minMillis = min.toMillis
			maxMillis = max.toMillis
		} yield ThreadLocalRandom.current.nextLong(minMillis, maxMillis) millis
	}

	def pause(duration: String, unit: TimeUnit = TimeUnit.SECONDS): B = pause(durationExpression(duration, unit))

	def pause(min: Duration, max: Duration): B = pause(durationExpression(min, max) _)

	def pause(min: String, max: String, unit: TimeUnit): B = pause(durationExpression(min, max, unit) _)

	def pause(min: Expression[Duration], max: Expression[Duration]): B = pause(durationExpression(min, max) _)

	def pause(duration: Expression[Duration]): B = exec(new PauseBuilder(duration))

	def pace(duration: String, unit: TimeUnit = TimeUnit.SECONDS): B = {
		pace(durationExpression(duration, unit))
	}

	def pace(min: Duration, max: Duration): B = {
		pace(durationExpression(min, max) _)
	}

	def pace(min: String, max: String, unit: TimeUnit): B = {
		pace(durationExpression(min, max, unit) _)
	}

	def pace(min: Expression[Duration], max: Expression[Duration]): B = {
		pace(durationExpression(min, max) _)
	}

	def pace(duration: Expression[Duration]): B = {
		pace(duration, UUID.randomUUID.toString)
	}

	def pace(duration: Expression[Duration], counter: String): B = {
		exec(new PaceBuilder(duration, counter))
	}

	def rendezVous(users: Int): B = exec(new RendezVousBuilder(users))
}

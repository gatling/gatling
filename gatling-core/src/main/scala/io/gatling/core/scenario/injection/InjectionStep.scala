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
package io.gatling.core.scenario.injection

import scala.concurrent.duration.{ DurationLong, FiniteDuration }
import scala.math.{ abs, sqrt }

trait InjectionStep {
	/**
	 * Iterator of time deltas in between any injected user and the beginning of the simulation
	 */
	def chain(iterator: Iterator[FiniteDuration]): Iterator[FiniteDuration]

	/**
	 * Number of users to inject
	 */
	def users: Int
}

/**
 * Ramp a given number of users over a given duration
 */
case class RampInjection(users: Int, duration: FiniteDuration) extends InjectionStep {
	require(users > 0, "The number of users must be a strictly positive value")

	override def chain(iterator: Iterator[FiniteDuration]): Iterator[FiniteDuration] = {
		val interval = duration / (users - 1).max(1)
		Iterator.iterate(0 milliseconds)(_ + interval).take(users) ++ iterator.map(_ + duration)
	}
}

/**
 * Inject users at constant rate : an other expression of a RampInjection
 */
case class ConstantRateInjection(rate: Double, duration: FiniteDuration) extends InjectionStep {
	val users = (duration.toSeconds * rate).toInt
	val ramp = RampInjection(users, duration)
	override def chain(iterator: Iterator[FiniteDuration]): Iterator[FiniteDuration] = ramp.chain(iterator)
}

/**
 * Don't injection any user for a given duration
 */
case class NothingForInjection(duration: FiniteDuration) extends InjectionStep {
	override def chain(iterator: Iterator[FiniteDuration]): Iterator[FiniteDuration] = iterator.map(_ + duration)
	override val users = 0
}

/**
 * Injection all the users at once
 */
case class AtOnceInjection(users: Int) extends InjectionStep {
	require(users > 0, "The number of users must be a strictly positive value")

	override def chain(iterator: Iterator[FiniteDuration]): Iterator[FiniteDuration] = Iterator.continually(0 milliseconds).take(users) ++ iterator
}

/**
 * The injection scheduling follows this equation
 * u = r1*t + (r2-r1)/(2*duration)*t²
 *
 * @r1 : initial injection rate in users/seconds
 * @r2 : final injection rate in users/seconds
 * @duration : injection duration
 */
case class RampRateInjection(r1: Double, r2: Double, duration: FiniteDuration) extends InjectionStep {
	require(r1 > 0 && r2 > 0, "injection rates must be strictly positive values")

	override val users = ((r1 + (r2 - r1) / 2) * duration.toSeconds).toInt

	override def chain(iterator: Iterator[FiniteDuration]): Iterator[FiniteDuration] = {
		val a = (r2 - r1) / (2 * duration.toSeconds)
		val b = r1
		val b2 = r1 * r1

		def userScheduling(u: Int) = {
			val c = -u
			val delta = b2 - 4 * a * c

			val t = (-b + sqrt(delta)) / (2 * a)
			(t * 1000).toLong milliseconds
		}

		Iterator.range(0, users).map(userScheduling(_)) ++ iterator.map(_ + duration)
	}
}

/**
 *  Inject users thru separated steps until reaching the total amount of users
 */
case class SplitInjection(possibleUsers: Int, step: InjectionStep, separator: InjectionStep) extends InjectionStep {
	private val stepUsers = step.users

	val users = {
		if (possibleUsers > stepUsers)
			possibleUsers - (possibleUsers - stepUsers) % (stepUsers + separator.users)
		else 0
	}

	override def chain(iterator: Iterator[FiniteDuration]) = {
		if (possibleUsers > stepUsers) {
			val separatorUsers = separator.users
			val n = (possibleUsers - stepUsers) / (stepUsers + separatorUsers)
			val lastScheduling = step.chain(iterator)
			(1 to n).foldRight(lastScheduling)((_, iterator) => step.chain(separator.chain(iterator)))
		} else
			iterator
	}
}

/**
 * Injection rate following a Dirac delta function
 *
 * numberOfInjectedUsers(t) = u(t)
 *                          = ∫δ(t)
 *                          = Heaviside(t)
 *                          = 1/2 + 1/2*erf(k*t)
 *                          (good numerical approximation)
 */
case class DiracInjection(users: Int, duration: FiniteDuration) extends InjectionStep {
	import io.gatling.core.math.Erf.erfinv

	override def chain(iterator: Iterator[FiniteDuration]) = {
		def heavisideInv(u: Double) = {
			val x = u / (users + 1)
			erfinv(2 * x - 1)
		}

		val t0 = abs(heavisideInv(1))
		val d = t0 * 2
		val k = duration.toMillis / d

		Iterator.range(1, users + 1).map(heavisideInv(_)).map(t => (k * (t + t0)).toLong milliseconds)
	}
}

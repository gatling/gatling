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
package com.excilys.ebi.gatling.core.scenario.injection

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.math.{ pow, sqrt }

trait InjectionStrategy {
	/**
	 * Time delta in between any injected user and the beginning of the injection
	 */
	def scheduling: Stream[FiniteDuration]

	/**
	 * Injection duration
	 */
	val duration: FiniteDuration

	/**
	 * Number of users to inject
	 */
	val users: Int
}

class RampInjection(val users: Int, val duration: FiniteDuration) extends InjectionStrategy {
	require(users > 0, "The number of users must be a strictly posivite value")

	override def toString = s"Ramp ${users} users for ${duration}"
	override def scheduling: Stream[FiniteDuration] = {
		val interval = duration / (users - 1).max(1)
		for (n <- Stream.range(0, users)) yield n * interval
	}
}

class DelayInjection(val duration: FiniteDuration) extends InjectionStrategy {
	override def toString = s"Delay for $duration"
	override def scheduling: Stream[FiniteDuration] = Stream.empty
	override val users = 0
}

class PeakInjection(val users: Int) extends InjectionStrategy {
	require(users > 0, "The number of users must be a strictly posivite value")

	override def toString = s"Peak $users users"
	override def scheduling: Stream[FiniteDuration] = Stream.continually(0 milliseconds).take(users)
	override val duration: FiniteDuration = 0 millisecond
}

/**
 * @r1 : initial injection rate in users/seconds
 * @r2 : final injection rate in users/seconds
 * @duration : injection duration
 *
 * The injection schedule follows this equation
 * u = r1*t + (r2-r1)/(2*duration)*t²
 *
 */
class RampRateInjection(r1: Double, r2: Double, val duration: FiniteDuration) extends InjectionStrategy {
	require(r1 > 0, "injection rates must be strictly positive values")
	require(r2 > 0, "injection rates must be strictly positive values")

	override def toString = s"Ramp rate from $r1 to $r2 users/seconds for $duration"
	override val users = ((r1 + (r2 - r1) / 2) * duration.toSeconds).toInt

	override def scheduling: Stream[FiniteDuration] = {
		val a = (r2 - r1) / (2 * duration.toSeconds)
		val b = r1
		val b2 = pow(r1, 2)

		def userScheduling(u: Int) = {
			val c = -u
			val delta = b2 - 4 * a * c

			val t = (-b + sqrt(delta)) / (2 * a)
			new FiniteDuration((t * 1000).toLong, TimeUnit.MILLISECONDS)
		}

		for (i <- Stream.range(0, users)) yield userScheduling(i)
	}
}
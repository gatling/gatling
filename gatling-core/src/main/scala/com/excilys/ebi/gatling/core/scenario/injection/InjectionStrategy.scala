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

import scala.concurrent.duration._

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
	override def toString = s"Ramp ${users} users for ${duration}"
	override def scheduling: Stream[FiniteDuration] = {
		val interval = duration / users
		for (n <- Stream.range(0, users)) yield n * interval
	}
}

class DelayInjection(val duration: FiniteDuration) extends InjectionStrategy {
	override def toString = s"Delay for $duration"
	override def scheduling: Stream[FiniteDuration] = Stream.empty
	override val users = 0
}

class PeakInjection(val users: Int) extends InjectionStrategy {
	override def toString = s"Peak $users users"
	override def scheduling: Stream[FiniteDuration] = Stream.continually(0 milliseconds).take(users)
	override val duration: FiniteDuration = 0 millisecond
}

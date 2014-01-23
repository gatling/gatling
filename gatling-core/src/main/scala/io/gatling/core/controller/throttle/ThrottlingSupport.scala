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
package io.gatling.core.controller.throttle

import scala.annotation.tailrec
import scala.concurrent.duration.Duration

trait ThrottleStep {

	val durationInSec: Long
	def target(previousLastValue: Int): Int
	def rps(time: Long, previousLastValue: Int): Int
}

case class ReachIntermediate(target: Int, history: List[ThrottleStep]) {
	def in(duration: Duration) = ThrottlingBuilder(Reach(target, duration) :: history)
}

case class Reach(target: Int, duration: Duration) extends ThrottleStep {
	val durationInSec = duration.toSeconds
	def target(previousLastValue: Int) = target
	def rps(time: Long, previousLastValue: Int): Int = ((target - previousLastValue) * time / durationInSec + previousLastValue).toInt
}

case class Hold(duration: Duration) extends ThrottleStep {
	val durationInSec = duration.toSeconds
	def target(previousLastValue: Int) = previousLastValue
	def rps(time: Long, previousLastValue: Int) = previousLastValue
}

case class Jump(target: Int) extends ThrottleStep {
	val durationInSec = 0L
	def target(previousLastValue: Int) = target
	def rps(time: Long, previousLastValue: Int) = 0
}

trait ThrottlingSupport {
	def steps: List[ThrottleStep] = Nil
	def reachRps(target: Int) = ReachIntermediate(target, steps)
	def holdFor(duration: Duration) = ThrottlingBuilder(Hold(duration) :: steps)
	def jumpToRps(target: Int) = ThrottlingBuilder(Jump(target) :: steps)
}

case class ThrottlingBuilder(override val steps: List[ThrottleStep]) extends ThrottlingSupport {

	def build: (Long => Int) = {
		@tailrec
		def valueAt(steps: List[ThrottleStep], pendingTime: Long, previousLastValue: Int): Int = steps match {
			case Nil => Int.MaxValue
			case head :: tail =>
				if (pendingTime < head.durationInSec)
					head.rps(pendingTime, previousLastValue)
				else
					valueAt(tail, pendingTime - head.durationInSec, head.target(previousLastValue))
		}

		val reversedSteps = steps.reverse
		(now: Long) => valueAt(reversedSteps, now, 0)
	}
}

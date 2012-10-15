/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.structure

import com.excilys.ebi.gatling.core.action.builder.{ CustomPauseActionBuilder, ExpPauseActionBuilder, PauseActionBuilder }

import akka.util.Duration
import akka.util.duration.longToDurationLong

trait Pauses[B] extends Execs[B] {

	/**
	 * Method used to define a pause
	 *
	 * @param duration the time for which the user waits/thinks, in seconds
	 * @return a new builder with a pause added to its actions
	 */
	def pause(duration: Long): B = pause(duration seconds, None)

	/**
	 * Method used to define a random pause in seconds
	 *
	 * @param minDuration the minimum value of the pause, in seconds
	 * @param maxDuration the maximum value of the pause, in seconds
	 * @return a new builder with a pause added to its actions
	 */
	def pause(minDuration: Long, maxDuration: Long): B = pause(minDuration seconds, Some(maxDuration seconds))

	/**
	 * Method used to define a random pause in seconds
	 *
	 * @param minDuration the minimum duration of the pause
	 * @param maxDuration the maximum duration of the pause
	 * @return a new builder with a pause added to its actions
	 */
	def pause(minDuration: Duration, maxDuration: Duration): B = pause(minDuration, Some(maxDuration))

	/**
	 * Method used to define a uniformly-distributed random pause
	 *
	 * @param minDuration the minimum value of the pause
	 * @param maxDuration the maximum value of the pause
	 * @return a new builder with a pause added to its actions
	 */
	def pause(minDuration: Duration, maxDuration: Option[Duration] = None): B = newInstance(PauseActionBuilder(minDuration, maxDuration) :: actionBuilders)

	/**
	 * Method used to define drawn from an exponential distribution with the specified mean duration.
	 *
	 * @param meanDuration the mean duration of the pause, in seconds
	 * @return a new builder with a pause added to its actions
	 */
	def pauseExp(meanDuration: Long): B = newInstance(ExpPauseActionBuilder(meanDuration seconds) :: actionBuilders)

	/**
	 * Method used to define drawn from an exponential distribution with the specified mean duration.
	 *
	 * @param meanDuration the mean duration of the pause
	 * @return a new builder with a pause added to its actions
	 */
	def pauseExp(meanDuration: Duration): B = newInstance(ExpPauseActionBuilder(meanDuration) :: actionBuilders)

	/**
	 * Define a pause with a custom strategy
	 *
	 * @param delayGenerator the strategy for computing the pauses, in milliseconds
	 * @return a new builder with a pause added to its actions
	 */
	def pauseCustom(delayGenerator: () => Long): B = newInstance(CustomPauseActionBuilder(delayGenerator) :: actionBuilders)
}
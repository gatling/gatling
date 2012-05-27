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
package com.excilys.ebi.gatling.core.action.builder

import java.util.concurrent.TimeUnit

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.action.UniformPauseAction
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry

import akka.actor.{ Props, ActorRef }

object PauseActionBuilder {

	/**
	 * Creates an initialized PauseActionBuilder with time unit in Seconds
	 */
	def pauseActionBuilder = new PauseActionBuilder(0, None, TimeUnit.SECONDS, null)
}

/**
 * Builder for UniformPauseAction
 *
 * @constructor create a new PauseActionBuilder
 * @param minDuration minimum duration of the generated pause
 * @param maxDuration maximum duration of the generated pause
 * @param timeUnit time unit of the duration of the generated pause
 * @param next action that will be executed after the generated pause
 */
class PauseActionBuilder(minDuration: Long, maxDuration: Option[Long] = None, timeUnit: TimeUnit, next: ActorRef) extends ActionBuilder {

	/**
	 * Adds minDuration to builder
	 *
	 * @param minDuration the minimum duration of the pause
	 * @return a new builder with minDuration set
	 */
	def withMinDuration(minDuration: Long) = new PauseActionBuilder(minDuration, maxDuration, timeUnit, next)

	/**
	 * Adds maxDuration to builder
	 *
	 * @param maxDuration the maximum duration of the pause
	 * @return a new builder with maxDuration set
	 */
	def withMaxDuration(maxDuration: Option[Long]) = new PauseActionBuilder(minDuration, maxDuration, timeUnit, next)

	/**
	 * Adds duration to builder
	 *
	 * @param duration the duration of the pause
	 * @return a new builder with duration set
	 */
	def withDuration(duration: Long) = new PauseActionBuilder(duration, None, timeUnit, next)

	/**
	 * Adds timeUnit to builder
	 *
	 * @param timeUnit time unit of the duration
	 * @return a new builder with timeUnit set
	 */
	def withTimeUnit(timeUnit: TimeUnit) = new PauseActionBuilder(minDuration, maxDuration, timeUnit, next)

	def withNext(next: ActorRef) = new PauseActionBuilder(minDuration, maxDuration, timeUnit, next)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = system.actorOf(Props(new UniformPauseAction(next, minDuration, maxDuration, timeUnit)))
}

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
package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.{ PauseAction, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.util.NumberHelper.createUniformRandomLongGenerator

import akka.actor.{ ActorRef, Props }
import akka.util.Duration

object PauseActionBuilder {

	/**
	 * Creates an initialized PauseActionBuilder with time unit in Seconds
	 */
	def apply(minDuration: Duration, maxDuration: Option[Duration]) = new PauseActionBuilder(minDuration, maxDuration, null)
}

/**
 * Builder for the 'pause' action.
 *
 * @constructor create a new PauseActionBuilder
 * @param minDuration minimum duration of the generated pause
 * @param maxDuration maximum duration of the generated pause
 * @param next action that will be executed after the generated pause
 */
class PauseActionBuilder(minDuration: Duration, maxDuration: Option[Duration] = None, next: ActorRef) extends ActionBuilder {

	def withNext(next: ActorRef) = new PauseActionBuilder(minDuration, maxDuration, next)

	def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {
		val minDurationInMillis = minDuration.toMillis
		val maxDurationInMillis = maxDuration.map(_.toMillis)

		val delayGenerator: () => Long = maxDurationInMillis.map(
			createUniformRandomLongGenerator(minDurationInMillis, _))
			.getOrElse(() => minDurationInMillis)
		system.actorOf(Props(new PauseAction(next, delayGenerator)))
	}
}

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

import scala.concurrent.duration.Duration

import com.excilys.ebi.gatling.core.action.{ Pause, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.util.NumberHelper.createUniformRandomLongGenerator

import akka.actor.{ ActorRef, Props }

/**
 * Builder for the 'pause' action.
 *
 * @constructor create a new PauseBuilder
 * @param minDuration minimum duration of the generated pause
 * @param maxDuration maximum duration of the generated pause
 */
class PauseBuilder(minDuration: Duration, maxDuration: Option[Duration] = None) extends ActionBuilder {

	def build(next: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {
		val minDurationInMillis = minDuration.toMillis
		val maxDurationInMillis = maxDuration.map(_.toMillis)

		val delayGenerator: () => Long = maxDurationInMillis.map(
			createUniformRandomLongGenerator(minDurationInMillis, _))
			.getOrElse(() => minDurationInMillis)
		system.actorOf(Props(new Pause(delayGenerator, next)))
	}
}

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
package com.excilys.ebi.gatling.core.action.builder

import scala.concurrent.duration.Duration

import com.excilys.ebi.gatling.core.action.{ Pause, system }
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.session.{ Expression, Session }
import com.excilys.ebi.gatling.core.util.NumberHelper.createUniformRandomLongGenerator

import akka.actor.{ ActorRef, Props }
import scalaz.Validation

/**
 * Builder for the 'pause' action.
 *
 * @constructor create a new PauseBuilder
 * @param minDuration minimum duration of the generated pause
 * @param maxDuration maximum duration of the generated pause
 */
class PauseBuilder(minDuration: Expression[Duration], maxDurationOption: Option[Expression[Duration]] = None) extends ActionBuilder {

	def build(next: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {

		def delayGenerator(session: Session): Validation[String, Long] = {
			val resolvedMinDurationInMillis = minDuration(session).map(_.toMillis)
			maxDurationOption.map(maxDuration => {
				val resolvedMaxDurationInMillis = maxDuration(session).map(_.toMillis)
				for (min <- resolvedMinDurationInMillis; max <- resolvedMaxDurationInMillis)
					yield createUniformRandomLongGenerator(min, max)()
			}).getOrElse(resolvedMinDurationInMillis)
		}

		system.actorOf(Props(new Pause(delayGenerator, next)))
	}
}

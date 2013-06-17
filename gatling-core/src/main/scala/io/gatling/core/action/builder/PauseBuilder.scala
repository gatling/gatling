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
package io.gatling.core.action.builder

import scala.concurrent.duration.Duration

import io.gatling.core.action.{ Pause, system }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.NumberHelper.createUniformRandomLongGenerator
import io.gatling.core.validation.Validation

import akka.actor.{ ActorRef, Props }

/**
 * Builder for the 'pause' action.
 *
 * @constructor create a new PauseBuilder
 * @param minDuration minimum duration of the generated pause
 * @param maxDuration maximum duration of the generated pause
 */
class PauseBuilder(minDuration: Expression[Duration], maxDurationOption: Option[Expression[Duration]] = None) extends ActionBuilder {

	def build(next: ActorRef) = {

		val pauseDuration = maxDurationOption match {
			case Some(maxDuration) => (session: Session) => {
				for {
					min <- minDuration(session)
					max <- maxDuration(session)
				} yield createUniformRandomLongGenerator(min.toMillis, max.toMillis)()
			}

			case None => (session: Session) => minDuration(session).map(_.toMillis)
		}

		system.actorOf(Props(new Pause(pauseDuration, next)))
	}
}

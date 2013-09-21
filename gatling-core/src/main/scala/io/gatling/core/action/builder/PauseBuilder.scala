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

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import io.gatling.core.action.Pause
import io.gatling.core.config.ProtocolRegistry
import io.gatling.core.pause.{ Disabled, PauseProtocol }
import io.gatling.core.session.Expression

/**
 * Builder for the 'pause' action.
 *
 * @constructor create a new PauseBuilder
 * @param duration mean duration of the generated pause
 */
class PauseBuilder(duration: Expression[Duration]) extends ActionBuilder {

	def build(next: ActorRef, protocolRegistry: ProtocolRegistry) = {

		protocolRegistry.getProtocol(PauseProtocol.default).pauseType match {
			case Disabled => next
			case pauseType =>
				val generator = pauseType.generator(duration)
				actor(new Pause(generator, next))
		}
	}
}

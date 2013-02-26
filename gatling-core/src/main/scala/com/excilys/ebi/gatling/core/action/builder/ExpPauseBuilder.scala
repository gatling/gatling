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
import com.excilys.ebi.gatling.core.util.NumberHelper.createExpRandomLongGenerator
import com.excilys.ebi.gatling.core.session.Session
import akka.actor.{ ActorRef, Props }
import scalaz.{ Validation, Success }

/**
 * Builder for the 'pauseExp' action.  Creates PauseActions for a user with a delay coming from
 * an exponential distribution with the specified mean duration.
 *
 * @constructor create a new ExpPauseActionBuilder
 * @param meanDuration mean duration of the generated pause
 */
class ExpPauseBuilder(meanDuration: Duration) extends ActionBuilder {

	def build(next: ActorRef, protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {
		val meanDurationInMillis = meanDuration.toMillis
		val delayGenerator: (Session) => Validation[String, Long] = (session:Session) => Success(createExpRandomLongGenerator(meanDurationInMillis)())

		system.actorOf(Props(new Pause(delayGenerator, next)))
	}
}

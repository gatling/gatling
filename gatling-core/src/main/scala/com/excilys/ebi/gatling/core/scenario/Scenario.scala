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
package com.excilys.ebi.gatling.core.scenario

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.scenario.configuration.ScenarioConfiguration
import com.excilys.ebi.gatling.core.session.Session

import akka.actor.ActorRef
import akka.util.duration.longToDurationLong

class Scenario(val name: String, entryPoint: ActorRef, val configuration: ScenarioConfiguration) {

	def run(userIdStart: Int) {

		def doRun {

			def newSession(i: Int) = new Session(name, i + userIdStart)

			if (configuration.users == 1) {
				// if single user, execute right now
				entryPoint ! newSession(1)

			} else {
				configuration.ramp.map { duration =>
					val period = duration.toMillis.toDouble / (configuration.users - 1)
					for (i <- 1 to configuration.users) system.scheduler.scheduleOnce((period * (i - 1)).toInt milliseconds, entryPoint, newSession(i))

				}.getOrElse {
					for (i <- 1 to configuration.users) entryPoint ! newSession(i)
				}
			}
		}

		configuration.delay
			.map(system.scheduler.scheduleOnce(_)(doRun))
			.getOrElse(doRun)
	}
}
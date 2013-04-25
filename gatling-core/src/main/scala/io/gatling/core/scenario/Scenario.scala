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
package io.gatling.core.scenario

import scala.concurrent.duration._

import io.gatling.core.action.system
import io.gatling.core.scenario.configuration.ScenarioConfiguration
import io.gatling.core.scenario.injection.InjectionStep
import io.gatling.core.session.Session

import akka.actor.ActorRef

class Scenario(val name: String, entryPoint: ActorRef, val configuration: ScenarioConfiguration) {

	def run(userIdStart: Int) {
		import system.dispatcher

		val scheduler = system.scheduler
		val zeroMs = 0 millisecond
		def newSession(i: Int) = Session(name, i + userIdStart)

		val allUsers = configuration.injections.foldRight(Iterator.empty: Iterator[FiniteDuration]) { (step, iterator) => step.chain(iterator) }

		allUsers.zipWithIndex.foreach {
			case (startingTime, index) =>
				if (startingTime == zeroMs) entryPoint ! newSession(index)
				else scheduler.scheduleOnce(startingTime, entryPoint, newSession(index))
		}
	}
}
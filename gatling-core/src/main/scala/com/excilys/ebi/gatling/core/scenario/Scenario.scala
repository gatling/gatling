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
package com.excilys.ebi.gatling.core.scenario

import scala.concurrent.duration.DurationInt

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.scenario.configuration.ScenarioConfiguration
import com.excilys.ebi.gatling.core.scenario.injection.InjectionStrategy
import com.excilys.ebi.gatling.core.session.Session

import akka.actor.ActorRef

class Scenario(val name: String, entryPoint: ActorRef, val configuration: ScenarioConfiguration) {

	def run(userIdStart: Int) {
		import system.dispatcher

		val scheduler = system.scheduler
		val zeroMs = 0 millisecond

		def newSession(i: Int) = Session(name, i + userIdStart)

		def doRun(injec: InjectionStrategy, partialUserSum: Int) = injec.scheduling.zipWithIndex.foreach {
			case (startingTime, index) =>
				if (startingTime == zeroMs) entryPoint ! newSession(partialUserSum + index)
				else scheduler.scheduleOnce(startingTime, entryPoint, newSession(partialUserSum + index))
		}

		val injections = configuration.injections
		val injectionTimeFrames = injections.foldLeft(List(zeroMs))((acc, injec) => acc.head + injec.duration :: acc).reverse
		val partialUserSums = injections.foldLeft(List(0))((acc, injec) => acc.head + injec.users :: acc).reverse

		(injections, injectionTimeFrames, partialUserSums).zipped.foreach {
			case (injec, startingTime, partialUserSum) =>
				if (startingTime == zeroMs) doRun(injec, partialUserSum)
				else scheduler.scheduleOnce(startingTime)(doRun(injec, partialUserSum))
		}

	}
}
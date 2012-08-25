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
package com.excilys.ebi.gatling.app

import org.joda.time.DateTime.now

import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.scenario.configuration.Simulation

class Selection(simulationClasses: List[Class[Simulation]], id: String, description: String) {

	def run: Seq[String] = {

		val size = simulationClasses.size

		Gatling.useActorSystem {
			for (i <- 0 until size) yield {
				val simulationClass = simulationClasses(i)
				val name = simulationClass.getName
				println(">> Running simulation (" + (i + 1) + "/" + size + ") - " + name)
				println("Simulation " + name + " started...")

				val runInfo = new RunRecord(now, id, description)

				val simulation = simulationClass.newInstance
				val configurations = simulation()
				new Runner(runInfo, configurations).run

				println("Simulation Finished.")
				runInfo.runUuid
			}
		}
	}
}
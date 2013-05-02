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
package com.excilys.ebi.gatling.core.runner

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

import org.joda.time.DateTime.now

import com.excilys.ebi.gatling.core.action.{ AkkaDefaults, system }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.result.terminator.Terminator
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.scenario.configuration.Simulation

import akka.dispatch.Await
import grizzled.slf4j.Logging

class Runner(selection: Selection) extends AkkaDefaults with Logging {

	def run: (String, Simulation) = {

		try {
			val simulationClass = selection.simulationClass
			println("Simulation " + simulationClass.getName + " started...")

			val runRecord = RunRecord(now, selection.simulationId, selection.description)

			val simulation = simulationClass.newInstance
			val scenarios = simulation.scenarios

			require(!scenarios.isEmpty, simulationClass.getName + " returned an empty scenario list. Did you forget to migrate your Simulations?")
			val scenarioNames = scenarios.map(_.name)
			require(scenarioNames.toSet.size == scenarioNames.size, "Scenario names must be unique but found " + scenarioNames)

			val totalNumberOfUsers = scenarios.map(_.configuration.users).sum
			info("Total number of users : " + totalNumberOfUsers)

			val terminatorLatch = new CountDownLatch(1)
			val init = Terminator
				.askInit(terminatorLatch, totalNumberOfUsers)
				.flatMap { _: Any => DataWriter.askInit(runRecord, scenarios) }

			Await.result(init, defaultTimeOut.duration)

			debug("Launching All Scenarios")

			scenarios.foldLeft(0) { (i, scenario) =>
				scenario.run(i)
				i + scenario.configuration.users
			}
			debug("Finished Launching scenarios executions")

			terminatorLatch.await(configuration.core.timeOut.simulation, SECONDS)
			println("Simulation finished.")

			(runRecord.runId, simulation)

		} finally {
			system.shutdown
		}
	}
}
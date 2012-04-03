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
package com.excilys.ebi.gatling.core.runner

import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.CountDownLatch

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.scenario.configuration.{ ScenarioConfigurationBuilder, ScenarioConfiguration }
import com.excilys.ebi.gatling.core.session.Session

import akka.actor.actorRef2Scala
import akka.actor.ActorRef
import akka.util.duration.longToDurationLong
import akka.util.Duration
import grizzled.slf4j.Logging

class Runner(runRecord: RunRecord, scenarioConfigurationBuilders: Seq[ScenarioConfigurationBuilder]) extends Logging {

	// stores all scenario configurations
	val scenarioConfigurations = for (i <- 0 until scenarioConfigurationBuilders.size) yield scenarioConfigurationBuilders(i).build(i + 1)

	// Counts the number of users
	val totalNumberOfUsers = scenarioConfigurations.map(_.users).sum

	// latch for determining when to send a PoisonPill to the DataWriter
	val userLatch = new CountDownLatch(totalNumberOfUsers)

	// latch for determining when to stop the application
	val dataWriterLatch = new CountDownLatch(1)

	// Builds all scenarios
	val scenarios = scenarioConfigurations.map { scenarioConfiguration =>
		val protocolRegistry = new ProtocolConfigurationRegistry(scenarioConfiguration.protocolConfigurations)
		scenarioConfiguration.scenarioBuilder.end(userLatch).build(protocolRegistry)
	}

	// Creates a List of Tuples with scenario configuration / scenario 
	val scenariosAndConfigurations = scenarioConfigurations zip scenarios

	info("Total number of users : " + totalNumberOfUsers)

	/**
	 * This method schedules the beginning of all scenarios
	 */
	def run {
		DataWriter.init(runRecord, dataWriterLatch)

		debug("Launching All Scenarios")

		// Scheduling all scenarios
		scenariosAndConfigurations.map {
			case (scenario, configuration) => {
				val (delayDuration, delayUnit) = scenario.delay
				system.scheduler.scheduleOnce(Duration(delayDuration, delayUnit), new Runnable {
					def run = startOneScenario(scenario, configuration.firstAction)
				})
			}
		}

		debug("Finished Launching scenarios executions")
		userLatch.await(configuration.simulationTimeOut, SECONDS)

		DataWriter.askShutDown
		dataWriterLatch.await(configuration.simulationTimeOut, SECONDS)

		debug("All scenarios finished, stoping actors")
	}

	/**
	 * This method starts one scenario
	 *
	 * @param configuration the configuration of the scenario
	 * @scenario the scenario that will be executed
	 * @return Nothing
	 */
	private def startOneScenario(configuration: ScenarioConfiguration, scenario: ActorRef) = {
		if (configuration.users == 1) {
			// if single user, execute right now
			scenario ! buildSession(configuration, 1)

		} else {
			// otherwise, schedule
			val (rampValue, rampUnit) = configuration.ramp
			// compute ramp period in millis so we can ramp less that one user per second
			val period = rampUnit.toMillis(rampValue) / (configuration.users - 1)

			for (i <- 1 to configuration.users)
				system.scheduler.scheduleOnce(period * (i - 1) milliseconds, scenario, buildSession(configuration, i))
		}
	}

	/**
	 * This method builds the session that will be sent to the first action of a scenario
	 *
	 * @param configuration the configuration of the scenario
	 * @param userId the id of the current user
	 * @return the built session
	 */
	private def buildSession(configuration: ScenarioConfiguration, userId: Int) = new Session(configuration.scenarioBuilder.name, userId)
}
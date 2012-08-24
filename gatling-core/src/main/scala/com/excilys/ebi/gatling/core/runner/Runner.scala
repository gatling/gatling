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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.core.result.message.{ RunRecord, ShortScenarioDescription }
import com.excilys.ebi.gatling.core.result.terminator.Terminator
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.scenario.configuration.{ ScenarioConfiguration, ScenarioConfigurationBuilder }
import com.excilys.ebi.gatling.core.session.Session

import akka.actor.ActorRef
import akka.util.Duration
import akka.util.duration.longToDurationLong
import grizzled.slf4j.Logging

class Runner(runRecord: RunRecord, scenarioConfigurationBuilders: Seq[ScenarioConfigurationBuilder]) extends Logging {

	// stores all scenario configurations
	val scenarioConfigurations = for (i <- 0 until scenarioConfigurationBuilders.size) yield scenarioConfigurationBuilders(i).build(i + 1)

	// Counts the number of users
	val totalNumberOfUsers = scenarioConfigurations.map(_.users).sum

	// A short description of the scenarios
	val shortScenarioDescriptions = scenarioConfigurations.map(scenarioConfiguration => ShortScenarioDescription(scenarioConfiguration.scenarioBuilder.name, scenarioConfiguration.users))

	// Builds all scenarios
	val scenarios = scenarioConfigurations.map { scenarioConfiguration =>
		val protocolRegistry = ProtocolConfigurationRegistry(scenarioConfiguration.protocolConfigurations)
		scenarioConfiguration.scenarioBuilder.end.build(protocolRegistry)
	}

	// Creates a List of Tuples with scenario configuration / scenario 
	val scenariosAndConfigurations = scenarioConfigurations zip scenarios

	info("Total number of users : " + totalNumberOfUsers)

	/**
	 * This method schedules the beginning of all scenarios
	 */
	def run {
		// latch for determining when to stop the application
		val terminatorLatch = new CountDownLatch(1)

		Terminator.init(terminatorLatch, totalNumberOfUsers)
		DataWriter.init(runRecord, shortScenarioDescriptions, configuration.encoding)

		debug("Launching All Scenarios")

		// Scheduling all scenarios
		scenariosAndConfigurations.map {
			case (scenario, configuration) => {
				val (delayDuration, delayUnit) = scenario.delay
				system.scheduler.scheduleOnce(Duration(delayDuration, delayUnit))(startOneScenario(scenario, configuration.firstAction))
			}
		}

		debug("Finished Launching scenarios executions")

		terminatorLatch.await(configuration.simulationTimeOut, SECONDS)

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

			val period = rampUnit.toMillis(rampValue).toDouble / (configuration.users - 1)

			for (i <- 1 to configuration.users)
				system.scheduler.scheduleOnce((period * (i - 1)).toInt milliseconds, scenario, buildSession(configuration, i))
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
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

import java.util.concurrent.{ TimeUnit, CountDownLatch }

import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.config.GatlingConfig
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.resource.ResourceRegistry
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.scenario.configuration.{ ScenarioConfigurationBuilder, ScenarioConfiguration }
import com.excilys.ebi.gatling.core.session.Session

import akka.actor.Actor.registry
import akka.actor.{ Scheduler, ActorRef }

object Runner {
	def runSim(startDate: DateTime)(scenarioConfigurations: ScenarioConfigurationBuilder*) = new Runner(startDate, scenarioConfigurations.toList).run
}

class Runner(startDate: DateTime, scenarioConfigurationBuilders: List[ScenarioConfigurationBuilder]) extends Logging {

	// stores all scenario configurations
	val scenarioConfigurations = for (i <- 1 to scenarioConfigurationBuilders.size) yield scenarioConfigurationBuilders(i - 1).build(i)

	// Counts the number of users
	val totalNumberOfUsers = scenarioConfigurations.map(_.users).sum

	// Initializes a countdown latch to determine when to stop the application
	val latch: CountDownLatch = new CountDownLatch(totalNumberOfUsers + 1)

	// Builds all scenarios
	val scenarios = scenarioConfigurations.map(_.scenarioBuilder.end(latch).build)

	// Creates a List of Tuples with scenario configuration / scenario 
	val scenariosAndConfigurations = scenarioConfigurations zip scenarios

	logger.info("Total number of users : {}", totalNumberOfUsers)

	/**
	 * This method schedules the beginning of all scenarios
	 */
	def run = {
		// Initialization of the data writer
		DataWriter.instance ! InitializeDataWriter(startDate, latch)

		logger.debug("Launching All Scenarios")

		// Scheduling all scenarios
		scenariosAndConfigurations.map {
			case (scenario, configuration) => {
				val (delayDuration, delayUnit) = scenario.delay
				Scheduler.scheduleOnce(() => {
					startOneScenario(scenario, configuration.firstAction)
				}, delayDuration, delayUnit)
			}
		}

		logger.debug("Finished Launching scenarios executions")
		latch.await(GatlingConfig.CONFIG_SIMULATION_TIMEOUT, TimeUnit.SECONDS)

		logger.debug("All scenarios finished, stoping actors")
		// Shuts down all actors
		registry.shutdownAll

		// Closes all the resources used during simulation
		ResourceRegistry.closeAll
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
			val session = buildSession(configuration, 1)
			scenario ! session
		} else {
			// otherwise, schedule
			val ramp = configuration.ramp
			// compute ramp period in millis so we can ramp less that one user per second
			val period = ramp._2.toMillis(ramp._1) / (configuration.users - 1)

			for (i <- 1 to configuration.users) {
				val session: Session = buildSession(configuration, i)
				Scheduler.scheduleOnce(() => scenario ! session, period * (i - 1), TimeUnit.MILLISECONDS)
			}
		}
	}

	/**
	 * This method builds the session that will be sent to the first action of a scenario
	 *
	 * @param configuration the configuration of the scenario
	 * @param userId the id of the current user
	 * @return the built session
	 */
	private def buildSession(configuration: ScenarioConfiguration, userId: Int) = {
		new Session(configuration.scenarioBuilder.name, userId).setProtocolConfig(configuration.protocolConfigurations)
	}
}
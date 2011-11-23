/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.runner

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.resource.ResourceRegistry
import com.excilys.ebi.gatling.core.result.writer.FileDataWriter
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.scenario.configuration.ScenarioConfigurationBuilder
import com.excilys.ebi.gatling.core.scenario.configuration.ScenarioConfiguration
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import java.util.Date
import akka.actor.Scheduler
import akka.actor.Actor.actorOf
import akka.actor.Actor.registry
import org.joda.time.DateTime
import com.excilys.ebi.gatling.core.scenario.Scenario
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import com.excilys.ebi.gatling.core.config.GatlingConfig

object Runner {
	def runSim(startDate: DateTime)(scenarioConfigurations: ScenarioConfigurationBuilder*) = new Runner(startDate, scenarioConfigurations.toList).run
}
class Runner(startDate: DateTime, scenarioConfigurationBuilders: List[ScenarioConfigurationBuilder]) extends Logging {

	val statWriter = actorOf[FileDataWriter].start

	// stores all scenario configurations
	val scenarioConfigurations = for (i <- 1 to scenarioConfigurationBuilders.size) yield scenarioConfigurationBuilders(i - 1).build(i)

	// Counts the number of users
	val totalNumberOfUsers = (for (configuration <- scenarioConfigurations) yield configuration.users).sum

	// Initializes a countdown latch to determine when to stop the application
	val latch: CountDownLatch = new CountDownLatch(totalNumberOfUsers + 1)

	// Builds all scenarios
	val scenarios = for (configuration <- scenarioConfigurations) yield configuration.scenarioBuilder.end(latch).build

	// Creates a List of Tuples with scenario configuration / scenario 
	val scenariosAndConfigurations = scenarioConfigurations zip scenarios

	logger.info("Total number of users : {}", totalNumberOfUsers)

	/**
	 * This method schedules the beginning of all scenarios
	 */
	def run = {

		// Initilization of the data writer
		statWriter ! InitializeDataWriter(startDate, latch)

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
	private def startOneScenario(configuration: ScenarioConfiguration, scenario: Action) = {
		if (configuration.users == 1) {
			// if single user, execute right now
			val context = buildContext(configuration, 1)
			scenario.execute(context)
		} else {
			// otherwise, schedule
			val ramp = configuration.ramp
			// compute ramp period in millis so we can ramp less that one user per second
			val period = ramp._2.toMillis(ramp._1) / (configuration.users - 1)

			for (i <- 1 to configuration.users) {
				val context: Context = buildContext(configuration, i)
				Scheduler.scheduleOnce(() => scenario.execute(context), period * (i - 1), TimeUnit.MILLISECONDS)
			}
		}
	}

	/**
	 * This method builds the context that will be sent to the first action of a scenario
	 *
	 * @param configuration the configuration of the scenario
	 * @param userId the id of the current user
	 * @return the built context
	 */
	private def buildContext(configuration: ScenarioConfiguration, userId: Int) = {
		val ctx = new Context(configuration.scenarioBuilder.name, userId, statWriter.getUuid)

		ctx.setProtocolConfig(configuration.protocolConfigurations)

		ctx
	}
}
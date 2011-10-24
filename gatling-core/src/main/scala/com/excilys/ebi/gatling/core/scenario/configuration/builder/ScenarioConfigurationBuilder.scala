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
package com.excilys.ebi.gatling.core.scenario.configuration.builder

import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.structure.builder.ScenarioBuilder
import com.excilys.ebi.gatling.core.scenario.configuration.ScenarioConfiguration

import java.util.concurrent.TimeUnit

/**
 * This class is used in the DSL to configure scenarios
 *
 * @param s the scenario to be configured
 * @param numUsers the number of users that will be simulated with this scenario
 * @param ramp the time in which all users must start
 * @param startTime the time at which the first user will start in the simulation
 * @param feeder the feeder associated with the scenario
 */
class ScenarioConfigurationBuilder(scenarioBuilder: ScenarioBuilder, usersValue: Int, rampValue: (Int, TimeUnit),
		delayValue: (Int, TimeUnit), feederValue: Option[Feeder]) {

	def this(scenarioBuilder: ScenarioBuilder) = {
		this(scenarioBuilder, 500, (0, TimeUnit.SECONDS), (0, TimeUnit.SECONDS), None)
	}

	/**
	 * Method used to set the number of users that will be executed
	 *
	 * @param nbUsers the number of users
	 * @return a new builder with the number of users set
	 */
	def users(nbUsers: Int) = new ScenarioConfigurationBuilder(scenarioBuilder, nbUsers, rampValue, delayValue, feederValue)

	/**
	 * Method used to set the ramp duration in seconds
	 *
	 * @param rampTime the duration of the ramp in seconds
	 * @return a new builder with ramp duration set
	 */
	def ramp(rampTime: Int): ScenarioConfigurationBuilder = ramp(rampTime, TimeUnit.SECONDS)

	/**
	 * Method used to set the ramp duration
	 *
	 * @param rampTime the duration of the ramp
	 * @param unit the time unit of the ramp duration
	 * @return a new builder with the ramp duration set
	 */
	def ramp(rampTime: Int, unit: TimeUnit) = new ScenarioConfigurationBuilder(scenarioBuilder, usersValue, (rampTime, unit), delayValue, feederValue)

	/**
	 * Method used to set the feeder used by the scenario
	 *
	 * @param feeder the feeder to be used by the scenario
	 * @return a new builder with the feeder set
	 */
	def feeder(feederValue: Feeder) = new ScenarioConfigurationBuilder(scenarioBuilder, usersValue, rampValue, delayValue, Some(feederValue))

	/**
	 * Method used to set the start time of the first user in the simulation in seconds
	 *
	 * @param startTime the time at which the first user will start, in seconds
	 * @return a new builder with the start time set
	 */
	def delay(delayValue: Int): ScenarioConfigurationBuilder = delay(delayValue, TimeUnit.SECONDS)

	/**
	 * Method used to set the start time of the first user in the simulation
	 *
	 * @param startTime the time at which the first user will start
	 * @param unit the unit of the start time
	 * @return a new builder with the start time set
	 */
	def delay(delayValue: Int, unit: TimeUnit) = new ScenarioConfigurationBuilder(scenarioBuilder, usersValue, rampValue, (delayValue, unit), feederValue)

	/**
	 * Builds the configuration of the scenario
	 *
	 * @return the configuration requested
	 */
	def build(scenarioId: Int): ScenarioConfiguration = new ScenarioConfiguration(scenarioId, scenarioBuilder, usersValue, rampValue, delayValue, feederValue)
}
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
package com.excilys.ebi.gatling.core.scenario.configuration

import java.util.concurrent.TimeUnit

import com.excilys.ebi.gatling.core.config.{ ProtocolConfiguration, ProtocolConfigurationRegistry }
import com.excilys.ebi.gatling.core.scenario.Scenario
import com.excilys.ebi.gatling.core.structure.ScenarioBuilder

import akka.util.Duration

private case class Attributes(scenarioBuilder: ScenarioBuilder, usersValue: Int, rampValue: Option[Duration], delayValue: Option[Duration], protocolConfigurationsValue: Seq[ProtocolConfiguration])

/**
 * This class is used in the DSL to configure scenarios
 *
 * @param s the scenario to be configured
 * @param numUsers the number of users that will be simulated with this scenario
 * @param ramp the time in which all users must start
 * @param startTime the time at which the first user will start in the simulation
 */
class ConfiguredScenarioBuilder(attributes: Attributes) {

	def this(scenarioBuilder: ScenarioBuilder) = this(Attributes(scenarioBuilder, 500, None, None, Seq.empty[ProtocolConfiguration]))

	/**
	 * Method used to set the number of users that will be executed
	 *
	 * @param nb the number of users
	 * @return a new builder with the number of users set
	 */
	def users(nb: Int) = new ConfiguredScenarioBuilder(attributes.copy(usersValue = nb))

	/**
	 * Method used to set the ramp duration
	 *
	 * @param duration the duration of the ramp
	 * @param unit the time unit of the ramp duration (defaults: seconds)
	 * @return a new builder with the ramp duration set
	 */
	def ramp(duration: Long, unit: TimeUnit = TimeUnit.SECONDS) = new ConfiguredScenarioBuilder(attributes.copy(rampValue = Some(Duration(duration, unit))))

	/**
	 * Method used to set the start time of the first user in the simulation
	 *
	 * @param duration the delay before the first user will start
	 * @param unit the unit of the delay (defaults: seconds)
	 * @return a new builder with the start time set
	 */
	def delay(duration: Long, unit: TimeUnit = TimeUnit.SECONDS) = new ConfiguredScenarioBuilder(attributes.copy(delayValue = Some(Duration(duration, unit))))

	/**
	 * Method used to set the different protocol configurations for this scenario
	 *
	 * @param protocolConfigurations the protocol configurations
	 * @return a new builder with the protocol configurations set
	 */
	def protocolConfig(protocolConfigurations: ProtocolConfiguration*) = new ConfiguredScenarioBuilder(attributes.copy(protocolConfigurationsValue = protocolConfigurations))

	/**
	 * Builds the scenario
	 *
	 * @return the scenario
	 */
	def build: Scenario = {
		val protocolRegistry = ProtocolConfigurationRegistry(attributes.protocolConfigurationsValue)
		val scenarioConfiguration = ScenarioConfiguration(attributes.usersValue, attributes.rampValue, attributes.delayValue, protocolRegistry)
		attributes.scenarioBuilder.build(scenarioConfiguration)
	}
}
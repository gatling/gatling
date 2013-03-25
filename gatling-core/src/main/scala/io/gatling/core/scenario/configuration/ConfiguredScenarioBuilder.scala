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
package io.gatling.core.scenario.configuration

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.config.{ ProtocolConfiguration, ProtocolConfigurationRegistry }
import io.gatling.core.scenario.Scenario
import io.gatling.core.scenario.injection.{ InjectionStep, AtOnceInjection, RampInjection, NothingForInjection }
import io.gatling.core.structure.ScenarioBuilder

private case class Attributes(scenarioBuilder: ScenarioBuilder, injections: List[InjectionStep] = Nil, protocolConfigurationsValue: Seq[ProtocolConfiguration] = Nil)

/**
 * This class is used in the DSL to configure scenarios
 *
 * @param s the scenario to be configured
 * @param numUsers the number of users that will be simulated with this scenario
 * @param ramp the time in which all users must start
 * @param startTime the time at which the first user will start in the simulation
 */
class ConfiguredScenarioBuilder(attributes: Attributes) {

	def this(scenarioBuilder: ScenarioBuilder) = this(Attributes(scenarioBuilder))

	def inject(is: InjectionStep, iss: InjectionStep*) = new ConfiguredScenarioBuilder(attributes.copy(injections = (attributes.injections ++ (is +: iss))))

	def protocolConfig(protocolConfigurations: ProtocolConfiguration*) = new ConfiguredScenarioBuilder(attributes.copy(protocolConfigurationsValue = protocolConfigurations))

	/**
	 * Builds the scenario
	 *
	 * @return the scenario
	 */
	def build: Scenario = {
		val protocolRegistry = ProtocolConfigurationRegistry(attributes.protocolConfigurationsValue)
		val scenarioConfiguration = ScenarioConfiguration(attributes.injections, protocolRegistry)
		attributes.scenarioBuilder.build(scenarioConfiguration)
	}
}
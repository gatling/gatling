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
package com.excilys.ebi.gatling.core.scenario.configuration

import scala.concurrent.duration.FiniteDuration

import com.excilys.ebi.gatling.core.config.{ ProtocolConfiguration, ProtocolConfigurationRegistry }
import com.excilys.ebi.gatling.core.scenario.Scenario
import com.excilys.ebi.gatling.core.scenario.injection.{ InjectionStrategy, PeakInjection, RampInjection, WaitInjection }
import com.excilys.ebi.gatling.core.structure.ScenarioBuilder

private case class Attributes(scenarioBuilder: ScenarioBuilder, injections: List[InjectionStrategy] = Nil, protocolConfigurationsValue: Seq[ProtocolConfiguration] = Nil)

class UserNumber(val number: Int)
class UsersPerSec(val rate: Double)

class RampDefinition(val users: Int, val duration: FiniteDuration)
class ConstantRateDefinition(val rate: Double, val duration: FiniteDuration)

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

	def ramp(rd: RampDefinition) = inject(new RampInjection(rd.users, rd.duration))
	def wait(d: FiniteDuration) = inject(new WaitInjection(d))
	def peak(users: UserNumber) = inject(new PeakInjection(users.number))
	def constantRate(crd: ConstantRateDefinition) = {
		val users = (crd.duration.toSeconds * crd.rate).toInt
		inject(new RampInjection(users, crd.duration))
	}
	// For custom injection strategies
	def inject(is: InjectionStrategy) = new ConfiguredScenarioBuilder(attributes.copy(injections = (is :: attributes.injections)))

	def protocolConfig(protocolConfigurations: ProtocolConfiguration*) = new ConfiguredScenarioBuilder(attributes.copy(protocolConfigurationsValue = protocolConfigurations))

	/**
	 * Builds the scenario
	 *
	 * @return the scenario
	 */
	def build: Scenario = {
		val protocolRegistry = ProtocolConfigurationRegistry(attributes.protocolConfigurationsValue)
		val scenarioConfiguration = ScenarioConfiguration(attributes.injections.reverse, protocolRegistry)
		attributes.scenarioBuilder.build(scenarioConfiguration)
	}
}
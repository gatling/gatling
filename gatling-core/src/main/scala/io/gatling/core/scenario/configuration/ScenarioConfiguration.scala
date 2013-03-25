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

import io.gatling.core.config.ProtocolConfigurationRegistry
import io.gatling.core.scenario.injection.InjectionStep

/**
 * This class represents the configuration of a scenario
 *
 * @param users the number of users that will behave as this scenario says
 * @param ramp the time in which all users must be launched
 * @param delay the time at which the engine will start in the scenario
 * @param protocolRegistry the registry for the protocols used in the scenario
 */
case class ScenarioConfiguration(
	injections: Seq[InjectionStep],
	protocolRegistry: ProtocolConfigurationRegistry) {

	val users = injections.map(_.users).sum
}
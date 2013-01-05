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

import scala.concurrent.duration.{ Duration, FiniteDuration }

import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry

/**
 * This class represents the configuration of a scenario
 *
 * @param users the number of users that will behave as this scenario says
 * @param ramp the time in which all users must be launched
 * @param delay the time at which the engine will start in the scenario
 * @param protocolRegistry the registry for the protocols used in the scenario
 */
case class ScenarioConfiguration(
	users: Int,
	ramp: Option[Duration],
	delay: Option[FiniteDuration],
	protocolRegistry: ProtocolConfigurationRegistry)
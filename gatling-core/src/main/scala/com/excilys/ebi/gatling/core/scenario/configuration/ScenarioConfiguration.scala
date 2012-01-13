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

import com.excilys.ebi.gatling.core.config.ProtocolConfiguration
import com.excilys.ebi.gatling.core.structure.ScenarioBuilder

/**
 * This class represents the configuration of a scenario
 *
 * @param scenarioId the id of the current scenario
 * @param scenarioBuilder the scenario
 * @param numberOfUsers the number of users that will behave as this scenario says
 * @param ramp the time in which all users must be launched
 * @param startTime the time at which the scenario will start in the simulation
 */
class ScenarioConfiguration(scenarioId: Int, val scenarioBuilder: ScenarioBuilder, val users: Int, val ramp: (Int, TimeUnit),
	val delay: (Int, TimeUnit), val protocolConfigurations: Seq[ProtocolConfiguration])
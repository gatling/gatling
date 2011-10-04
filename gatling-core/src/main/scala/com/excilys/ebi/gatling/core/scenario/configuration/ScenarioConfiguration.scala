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
package com.excilys.ebi.gatling.core.scenario.configuration

import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder._

import java.util.concurrent.TimeUnit

/**
 * This class represents the configuration of a scenario
 *
 * @param scenarioId the id of the current scenario
 * @param scenarioBuilder the scenario
 * @param numberOfUsers the number of users that will behave as this scenario says
 * @param ramp the time in which all users must be launched
 * @param startTime the time at which the scenario will start in the simulation
 * @param feeder a feeder that will be consumed by this scenario for each user
 */
class ScenarioConfiguration(val scenarioId: Int, val scenarioBuilder: ScenarioBuilder[_ <: ScenarioBuilder[_]], val numberOfUsers: Int, val ramp: (Int, TimeUnit),
                            val startTime: (Int, TimeUnit), val feeder: Option[Feeder])
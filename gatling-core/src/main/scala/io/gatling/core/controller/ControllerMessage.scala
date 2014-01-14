/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.controller

import scala.util.Try

import io.gatling.core.scenario.Simulation

sealed trait ControllerMessage
case class Run(simulation: Simulation, simulationId: String, description: String, timings: Timings) extends ControllerMessage
case class DataWritersInitialized(count: Try[Unit]) extends ControllerMessage
case object ForceTermination extends ControllerMessage
case class DataWritersTerminated(count: Try[Unit]) extends ControllerMessage
case object OneSecondTick extends ControllerMessage
case class ThrottledRequest(scenarioName: String, request: () => Unit) extends ControllerMessage

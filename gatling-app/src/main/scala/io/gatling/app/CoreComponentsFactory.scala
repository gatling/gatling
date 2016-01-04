/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.app

import io.gatling.core.CoreComponents
import io.gatling.core.action.Exit
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.Controller
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.stats.DataWritersStatsEngine
import io.gatling.core.stats.writer.RunMessage

import akka.actor.ActorSystem

private[gatling] object CoreComponentsFactory {

  def apply(configuration: GatlingConfiguration): CoreComponentsFactory = {
    //
    //
    //
    //
    //
    //
    new DefaultCoreComponentsFactory()(configuration)
  }
}

private[gatling] trait CoreComponentsFactory {

  def coreComponents(system: ActorSystem, simulationParams: SimulationParams, runMessage: RunMessage): CoreComponents

  def runResultProcessor: RunResultProcessor
}

private[gatling] class DefaultCoreComponentsFactory(implicit configuration: GatlingConfiguration) extends CoreComponentsFactory {

  def coreComponents(system: ActorSystem, simulationParams: SimulationParams, runMessage: RunMessage): CoreComponents = {
    val statsEngine = DataWritersStatsEngine(system, simulationParams, runMessage, configuration)
    val throttler = Throttler(system, simulationParams)
    val controller = system.actorOf(Controller.props(statsEngine, throttler, simulationParams, configuration), Controller.ControllerActorName)
    val exit = system.actorOf(Exit.props(controller, statsEngine), Exit.ExitActorName)

    CoreComponents(controller, throttler, statsEngine, exit)
  }

  def runResultProcessor: RunResultProcessor =
    new LogFileProcessor
}

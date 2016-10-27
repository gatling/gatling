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

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Try }

import io.gatling.commons.util.Ga
import io.gatling.commons.util.ClockSingleton._
import io.gatling.core.CoreComponents
import io.gatling.core.action.Exit
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.{ Controller, ControllerCommand }
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.{ Scenario, SimulationParams }
import io.gatling.core.stats.{ DataWritersStatsEngine, StatsEngine }
import io.gatling.core.stats.writer.RunMessage

import akka.actor.ActorSystem
import akka.pattern.ask
import com.typesafe.scalalogging.StrictLogging

private[app] object Runner {

  def apply(system: ActorSystem, configuration: GatlingConfiguration): Runner =
    configuration.resolve(
      // [fl]
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      // [fl]
      new Runner(system, configuration)
    )
}

private[gatling] class Runner(system: ActorSystem, configuration: GatlingConfiguration) extends StrictLogging {

  private[app] def run(selectedSimulationClass: SelectedSimulationClass): RunResult =
    configuration.core.directory.reportsOnly match {
      case Some(reportsOnly) => RunResult(reportsOnly, hasAssertions = true)
      case _ =>
        if (configuration.http.enableGA) Ga.send(configuration.core.version)
        run0(selectedSimulationClass)
    }

  protected def newStatsEngine(simulationParams: SimulationParams, runMessage: RunMessage): StatsEngine =
    DataWritersStatsEngine(system, simulationParams, runMessage, configuration)

  protected def run0(selectedSimulationClass: SelectedSimulationClass): RunResult = {
    logger.trace("Running")
    // important, initialize time reference
    loadClockSingleton()

    // ugly way to pass the configuration to the DSL
    io.gatling.core.Predef.configuration = configuration

    val selection = Selection(selectedSimulationClass, configuration)
    val simulation = selection.simulationClass.newInstance
    logger.trace("Simulation instantiated")
    val simulationParams = simulation.params(configuration)
    logger.trace("Simulation params built")

    simulation.executeBefore()
    logger.trace("Before hooks executed")

    val runMessage = RunMessage(simulationParams.name, selection.userDefinedSimulationId, selection.defaultSimulationId, nowMillis, selection.description)
    val statsEngine = newStatsEngine(simulationParams, runMessage)
    val throttler = Throttler(system, simulationParams)
    val controller = system.actorOf(Controller.props(statsEngine, throttler, simulationParams, configuration), Controller.ControllerActorName)
    val exit = new Exit(controller, statsEngine)
    val coreComponents = CoreComponents(controller, throttler, statsEngine, exit, configuration)
    logger.trace("CoreComponents instantiated")

    val scenarios = simulationParams.scenarios(system, coreComponents)

    System.gc()

    start(simulationParams, scenarios, coreComponents) match {
      case Failure(t) => throw t
      case _ =>
        simulation.executeAfter()
        logger.trace("After hooks executed")
        RunResult(runMessage.runId, simulationParams.assertions.nonEmpty)
    }
  }

  protected def start(simulationParams: SimulationParams, scenarios: List[Scenario], coreComponents: CoreComponents): Try[_] = {
    val timeout = Int.MaxValue.milliseconds - 10.seconds
    val start = nowMillis
    println(s"Simulation ${simulationParams.name} started...")
    logger.trace("Asking Controller to start")
    val whenRunDone: Future[Try[String]] = coreComponents.controller.ask(ControllerCommand.Start(scenarios))(timeout).mapTo[Try[String]]
    val runDone = Await.result(whenRunDone, timeout)
    println(s"Simulation ${simulationParams.name} completed in ${(nowMillis - start) / 1000} seconds")
    runDone
  }
}

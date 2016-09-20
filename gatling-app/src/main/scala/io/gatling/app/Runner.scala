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
import io.gatling.commons.util.TimeHelper._
import io.gatling.core.CoreComponents
import io.gatling.core.action.Exit
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.{ Controller, ControllerCommand }
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.stats.DataWritersStatsEngine
import io.gatling.core.stats.writer.RunMessage

import akka.actor.ActorSystem
import akka.pattern.ask

private[app] object Runner {

  def apply(configuration: GatlingConfiguration): Runner =
    configuration.resolve(
      // [fl]
      //
      //
      //
      //
      //
      // [fl]
      new Runner(configuration)
    )
}

private[gatling] class Runner(configuration: GatlingConfiguration) {

  private[app] def run(selectedSimulationClass: SelectedSimulationClass): RunResult =
    configuration.core.directory.reportsOnly match {
      case Some(reportsOnly) => RunResult(reportsOnly, hasAssertions = true)
      case _ =>
        if (configuration.http.enableGA) Ga.send(configuration.core.version)

        // start actor system before creating simulation instance, some components might need it (e.g. shutdown hook)
        val system = ActorSystem("GatlingSystem", GatlingConfiguration.loadActorSystemConfiguration())

        try {
          run(Selection(selectedSimulationClass, configuration), system)
        } finally {
          val whenTerminated = system.terminate()
          Await.result(whenTerminated, 2 seconds)
        }
    }

  protected def newCoreComponents(system: ActorSystem, simulationParams: SimulationParams, runMessage: RunMessage) = {
    val statsEngine = DataWritersStatsEngine(system, simulationParams, runMessage, configuration)
    val throttler = Throttler(system, simulationParams)
    val controller = system.actorOf(Controller.props(statsEngine, throttler, simulationParams, configuration), Controller.ControllerActorName)
    val exit = new Exit(controller, statsEngine)
    CoreComponents(controller, throttler, statsEngine, exit, configuration)
  }

  protected def run(selection: Selection, system: ActorSystem): RunResult = {

    // important, initialize time reference
    val timeRef = NanoTimeReference

    // ugly way to pass the configuration to the Simulation constructor
    io.gatling.core.Predef.configuration = configuration

    val simulation = selection.simulationClass.newInstance
    val simulationParams = simulation.params(configuration)

    simulation.executeBefore()

    val runMessage = RunMessage(simulationParams.name, selection.userDefinedSimulationId, selection.defaultSimulationId, nowMillis, selection.description)
    val coreComponents = newCoreComponents(system, simulationParams, runMessage)

    val scenarios = simulationParams.scenarios(system, coreComponents)

    System.gc()

    val timeout = Int.MaxValue.milliseconds - 10.seconds

    val start = nowMillis
    println(s"Simulation ${simulationParams.name} started...")
    val whenRunDone: Future[Try[String]] = coreComponents.controller.ask(ControllerCommand.Start(scenarios))(timeout).mapTo[Try[String]]
    val runDone = Await.result(whenRunDone, timeout)
    println(s"Simulation ${simulationParams.name} completed in ${(nowMillis - start) / 1000} seconds")

    runDone match {
      case Failure(t) => throw t
      case _ =>
        simulation.executeAfter()
        RunResult(runMessage.runId, simulationParams.assertions.nonEmpty)
    }
  }
}

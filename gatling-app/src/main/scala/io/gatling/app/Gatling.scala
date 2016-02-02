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

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Failure, Try }

import io.gatling.app.cli.{ StatusCode, ArgsParser }
import io.gatling.commons.util.{ Ga, StringHelper }
import io.gatling.commons.util.TimeHelper._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.ControllerCommand
import io.gatling.core.stats.writer.RunMessage

import akka.actor.ActorSystem
import akka.pattern.ask

/**
 * Object containing entry point of application
 */
object Gatling {

  def main(args: Array[String]): Unit = sys.exit(fromArgs(args, None))

  def fromMap(overrides: ConfigOverrides): Int = start(overrides, None)

  def fromArgs(args: Array[String], selectedSimulationClass: SelectedSimulationClass): Int =
    new ArgsParser(args).parseArguments match {
      case Left(overrides)   => start(overrides, selectedSimulationClass)
      case Right(statusCode) => statusCode.code
    }

  private[app] def start(overrides: ConfigOverrides, selectedSimulationClass: SelectedSimulationClass) = {

    val configuration = GatlingConfiguration.load(overrides)

    new Gatling(selectedSimulationClass, configuration).start.code
  }
}

private[app] class Gatling(selectedSimulationClass: SelectedSimulationClass, configuration: GatlingConfiguration) {

  def start: StatusCode = {
    StringHelper.checkSupportedJavaVersion()
    val coreComponentsFactory = CoreComponentsFactory(configuration)
    val runResult = runIfNecessary(coreComponentsFactory)
    coreComponentsFactory.runResultProcessor.processRunResult(runResult)
  }

  private def runIfNecessary(coreComponentsFactory: CoreComponentsFactory): RunResult =
    configuration.core.directory.reportsOnly match {
      case Some(reportsOnly) => RunResult(reportsOnly, hasAssertions = true)
      case _ =>
        if (configuration.http.enableGA) Ga.send(configuration.core.version)
        // -- Run Gatling -- //
        run(Selection(selectedSimulationClass, configuration), coreComponentsFactory)
    }

  private def run(selection: Selection, coreComponentsFactory: CoreComponentsFactory): RunResult = {

    // start actor system before creating simulation instance, some components might need it (e.g. shutdown hook)
    val system = ActorSystem("GatlingSystem", GatlingConfiguration.loadActorSystemConfiguration())

    try {
      val simulationClass = selection.simulationClass

      // important, initialize time reference
      val timeRef = NanoTimeReference

      // ugly way to pass the configuration to the Simulation constructor
      io.gatling.core.Predef.configuration = configuration

      val simulation = simulationClass.newInstance

      val simulationParams = simulation.params(configuration)

      simulationParams.beforeSteps.foreach(_.apply())

      val runMessage = RunMessage(selection.simulationClass.getName, selection.userDefinedSimulationId, selection.defaultSimulationId, nowMillis, selection.description)

      val coreComponents = coreComponentsFactory.coreComponents(system, simulationParams, runMessage)

      val scenarios = simulationParams.scenarios(system, coreComponents)

      System.gc()

      val timeout = Int.MaxValue.milliseconds - 10.seconds

      val start = nowMillis
      println(s"Simulation ${simulationClass.getName} started...")
      val runResult = coreComponents.controller.ask(ControllerCommand.Start(scenarios))(timeout).mapTo[Try[String]]
      val res = Await.result(runResult, timeout)
      println(s"Simulation ${simulationClass.getName} completed in ${(nowMillis - start) / 1000} seconds")

      res match {
        case Failure(t) => throw t
        case _ =>
          simulationParams.afterSteps.foreach(_.apply())
          RunResult(runMessage.runId, simulationParams.assertions.nonEmpty)
      }

    } finally {
      val whenTerminated = system.terminate()
      Await.result(whenTerminated, 2 seconds)
    }
  }
}

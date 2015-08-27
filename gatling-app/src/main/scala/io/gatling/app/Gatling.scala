/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import io.gatling.core.controller.Start
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

    implicit val configuration = GatlingConfiguration.load(overrides)

    new Gatling(selectedSimulationClass).start.code
  }
}

private[app] class Gatling(selectedSimulationClass: SelectedSimulationClass)(implicit configuration: GatlingConfiguration) {

  val coreComponentsFactory = CoreComponentsFactory(configuration)

  def start: StatusCode = {

    StringHelper.checkSupportedJavaVersion()

    val runResult = runIfNecessary
    coreComponentsFactory.resultsProcessor.processResults(runResult)
  }

  private def runIfNecessary: RunResult =
    configuration.core.directory.reportsOnly match {
      case Some(reportsOnly) => RunResult(reportsOnly, hasAssertions = true)
      case _ if configuration.http.enableGA =>
        Ga.send(configuration.core.version)
        // -- Run Gatling -- //
        run(Selection(selectedSimulationClass))
    }

  private def run(selection: Selection): RunResult = {

    // start actor system before creating simulation instance, some components might need it (e.g. shutdown hook)
    val system = ActorSystem("GatlingSystem", GatlingConfiguration.loadActorSystemConfiguration())

    try {
      val simulationClass = selection.simulationClass

      // important, initialize time reference
      val timeRef = NanoTimeReference

      // ugly way to pass the configuration to the Simulation constructor
      io.gatling.core.Predef.configuration = configuration

      val simulation = simulationClass.newInstance

      val simulationParams = simulation.params

      simulationParams.beforeSteps.foreach(_.apply())

      val runMessage = RunMessage(selection.simulationClass.getName, selection.simulationId, nowMillis, selection.description)

      val coreComponents = coreComponentsFactory.coreComponents(system, simulationParams, runMessage)

      val scenarios = simulationParams.scenarios(system, coreComponents)

      System.gc()

      val timeout = Int.MaxValue.milliseconds - 10.seconds

      println(s"Simulation ${simulationClass.getName} started...")
      val runResult = coreComponents.controller.ask(Start(scenarios))(timeout).mapTo[Try[String]]
      val res = Await.result(runResult, timeout)
      println(s"Simulation ${simulationClass.getName} completed")

      res match {
        case Failure(t) => throw t
        case _ =>
          simulationParams.afterSteps.foreach(_.apply())
          RunResult(runMessage.runId, simulationParams.assertions.nonEmpty)
      }

    } finally {
      system.shutdown()
      system.awaitTermination()
    }
  }
}

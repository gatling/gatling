/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util._
import io.gatling.core.CoreComponents
import io.gatling.core.action.Exit
import io.gatling.core.cli.GatlingArgs
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.{ Controller, ControllerCommand }
import io.gatling.core.controller.inject.{ Injector, ScenarioFlows }
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.{ Scenario, SimulationParams }
import io.gatling.core.stats.{ DataWritersStatsEngine, StatsEngine }
import io.gatling.core.stats.writer.RunMessage

import akka.actor.ActorSystem
import akka.pattern.ask
import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.EventLoopGroup

private[gatling] object Runner {
  def apply(system: ActorSystem, eventLoopGroup: EventLoopGroup, gatlingArgs: GatlingArgs, configuration: GatlingConfiguration): Runner =
    new Runner(system, eventLoopGroup, new DefaultClock, gatlingArgs, configuration)
}

private[gatling] class Runner(system: ActorSystem, eventLoopGroup: EventLoopGroup, clock: Clock, gatlingArgs: GatlingArgs, configuration: GatlingConfiguration)
    extends StrictLogging {
  private[app] final def run(): RunResult = {
    logger.trace("Running")

    displayVersionWarning()

    // ugly way to pass the configuration to the DSL
    io.gatling.core.Predef._configuration = configuration

    val selection = Selection(gatlingArgs)

    if (configuration.data.enableAnalytics) Analytics.send(selection.simulationClass, gatlingArgs.launcher, gatlingArgs.buildToolVersion)

    val (simulationParams, runMessage, coreComponents, scenarioFlows) = load(selection)

    start(simulationParams, scenarioFlows, coreComponents) match {
      case Failure(t) => throw t
      case _ =>
        simulationParams.after()
        logger.trace("After hook executed")
        new RunResult(runMessage.runId, simulationParams.assertions.nonEmpty)
    }
  }

  protected def newStatsEngine(simulationParams: SimulationParams, runMessage: RunMessage): StatsEngine =
    DataWritersStatsEngine(simulationParams, runMessage, system, clock, gatlingArgs.resultsDirectory, configuration)

  protected[gatling] def load(selection: Selection): (SimulationParams, RunMessage, CoreComponents, ScenarioFlows[String, Scenario]) = {
    val simulationParams = selection.simulationClass.params(configuration)
    logger.trace("Simulation params built")

    simulationParams.before()
    logger.trace("Before hook executed")

    val runMessage = RunMessage(
      simulationParams.name,
      selection.simulationId,
      clock.nowMillis,
      selection.description,
      GatlingVersion.ThisVersion.fullVersion,
      configuration.data.zoneId
    )
    val coreComponents = {
      val statsEngine = newStatsEngine(simulationParams, runMessage)
      val throttler = Throttler.newThrottler(system, simulationParams)
      val injector = Injector(system, eventLoopGroup, statsEngine, clock)
      val controller = system.actorOf(Controller.props(statsEngine, injector, throttler, simulationParams), Controller.ControllerActorName)
      val exit = new Exit(injector)
      new CoreComponents(system, eventLoopGroup, controller, throttler, statsEngine, clock, exit, configuration)
    }
    logger.trace("CoreComponents instantiated")

    val scenarioFlows = simulationParams.scenarioFlows(coreComponents)

    (simulationParams, runMessage, coreComponents, scenarioFlows)
  }

  protected[gatling] def start(
      simulationParams: SimulationParams,
      scenarioFlows: ScenarioFlows[String, Scenario],
      coreComponents: CoreComponents
  ): Try[_] = {
    val timeout = Int.MaxValue.milliseconds - 10.seconds
    val start = coreComponents.clock.nowMillis
    println(s"Simulation ${simulationParams.name} started...")
    logger.trace("Asking Controller to start")
    val whenRunDone: Future[Try[String]] = coreComponents.controller.ask(ControllerCommand.Start(scenarioFlows))(timeout).mapTo[Try[String]]
    val runDone = Await.result(whenRunDone, timeout)
    println(s"Simulation ${simulationParams.name} completed in ${(coreComponents.clock.nowMillis - start) / 1000} seconds")
    runDone
  }

  protected def displayVersionWarning(): Unit =
    GatlingVersion.LatestRelease.foreach { latest =>
      if (latest.fullVersion != GatlingVersion.ThisVersion.fullVersion && latest.releaseDate.isAfter(GatlingVersion.ThisVersion.releaseDate)) {
        println(s"Gatling ${latest.fullVersion} is available! (you're using ${GatlingVersion.ThisVersion.fullVersion})")
      }
    }
}

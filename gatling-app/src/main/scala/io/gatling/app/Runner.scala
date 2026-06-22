/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import java.lang.reflect.InvocationTargetException

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Failure, Try }

import io.gatling.commons.util._
import io.gatling.core.CoreComponents
import io.gatling.core.action.Exit
import io.gatling.core.actor.ActorSystem
import io.gatling.core.cli.GatlingArgs
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.Controller
import io.gatling.core.controller.inject.{ Injector, PopulationFlows }
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.{ Population, SimulationParams }
import io.gatling.core.stats.{ DataWritersStatsEngine, StatsEngine }
import io.gatling.core.stats.writer.RunMessage

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.EventLoopGroup

private[gatling] object Runner {
  def apply(system: ActorSystem, eventLoopGroup: EventLoopGroup, gatlingArgs: GatlingArgs, configuration: GatlingConfiguration): Runner =
    new Runner(system, eventLoopGroup, new DefaultClock, gatlingArgs, configuration)
}

private[gatling] class Runner(system: ActorSystem, eventLoopGroup: EventLoopGroup, clock: Clock, gatlingArgs: GatlingArgs, configuration: GatlingConfiguration)
    extends StrictLogging {
  private[app] final def run(): RunResult = {
    displayVersionWarning()

    val selection = Selection(gatlingArgs)

    val simulationParams = instantiateSimulation(selection, configuration)
    executeHook("before", simulationParams.before)

    val runMessage = RunMessage(
      simulationParams.name,
      selection.simulationId,
      clock.nowMillis,
      selection.description,
      GatlingVersion.ThisVersion.fullVersion,
      configuration.data.zoneId
    )

    val coreComponents = loadCoreComponents(simulationParams, runMessage)
    val populationFlows = loadPopulations(simulationParams, coreComponents)

    if (configuration.data.enableAnalytics) Analytics.send(selection.simulationClass, gatlingArgs.launcher, gatlingArgs.buildToolVersion)

    start(simulationParams, coreComponents, populationFlows) match {
      case Failure(t) =>
        // [e]
        //
        // [e]
        throw new GatlingLifecycleException.Injection(t)
      case _ =>
        executeHook("after", simulationParams.after)
        // [e]
        //
        //
        // [e]
        new RunResult(runMessage.runId, simulationParams.assertions.nonEmpty)
    }
  }

  protected def displayVersionWarning(): Unit =
    GatlingVersion.LatestRelease.foreach { latest =>
      if (latest.fullVersion != GatlingVersion.ThisVersion.fullVersion && latest.releaseDate.isAfter(GatlingVersion.ThisVersion.releaseDate)) {
        println(s"Gatling ${latest.fullVersion} is available! (you're using ${GatlingVersion.ThisVersion.fullVersion})")
      }
    }

  private final def instantiateSimulation(selection: Selection, configuration: GatlingConfiguration): SimulationParams = {
    // ugly way to pass the configuration to the DSL
    io.gatling.core.Predef._configuration = configuration

    try {
      selection.simulationClass.params
    } catch {
      case invocationTargetException: InvocationTargetException =>
        throw new GatlingLifecycleException.SimulationInstantiation(invocationTargetException.getTargetException)
      case t: Throwable =>
        // we also want to catch things like ExceptionInInitializerError which is Fatal
        throw new GatlingLifecycleException.SimulationInstantiation(t)
    }
  }

  private final def executeHook(name: String, hookO: Option[() => Unit]): Unit =
    GatlingLifecycleException.manage(t => new GatlingLifecycleException.HookExecution(name, t)) {
      hookO.foreach(_.apply())
    }

  private final def loadCoreComponents(simulationParams: SimulationParams, runMessage: RunMessage): CoreComponents = {
    val statsEngine = newStatsEngine(simulationParams, runMessage)
    val throttler = Throttler.actor(simulationParams.throttlings(configuration)).map(system.actorOf)
    val injector = system.actorOf(Injector.actor(eventLoopGroup, statsEngine, clock))
    val controller = system.actorOf(Controller.actor(statsEngine, injector, throttler, simulationParams))
    val exit = new Exit(injector)
    new CoreComponents(system, eventLoopGroup, controller, throttler, statsEngine, clock, exit, configuration)
  }

  protected def newStatsEngine(simulationParams: SimulationParams, runMessage: RunMessage): StatsEngine =
    DataWritersStatsEngine(simulationParams, runMessage, system, clock, gatlingArgs.resultsDirectory, configuration)

  private final def loadPopulations(simulationParams: SimulationParams, coreComponents: CoreComponents): PopulationFlows[String, Population] =
    GatlingLifecycleException.manage(t => new GatlingLifecycleException.ScenariosBuilding(t)) {
      simulationParams.populationFlows(coreComponents)
    }

  protected[gatling] def start(
      simulationParams: SimulationParams,
      coreComponents: CoreComponents,
      populationFlows: PopulationFlows[String, Population]
  ): Try[Unit] = {
    val timeout = Int.MaxValue.milliseconds - 10.seconds
    val start = coreComponents.clock.nowMillis
    logger.info(s"Simulation ${simulationParams.name} started...")
    val runDonePromise = coreComponents.controller.replyPromise[Unit](timeout)
    coreComponents.controller ! Controller.Command.Start(
      populationFlows = populationFlows,
      runDonePromise = runDonePromise
    )
    val runDone = Try(Await.result(runDonePromise.future, timeout))
    logger.info(s"Simulation ${simulationParams.name} completed in ${(coreComponents.clock.nowMillis - start) / 1000} seconds")
    runDone
  }

  // [e]
  //
  //
  // [e]
}

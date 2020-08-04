/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.{ Controller, ControllerCommand }
import io.gatling.core.controller.inject.Injector
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.{ Scenarios, SimulationParams }
import io.gatling.core.stats.{ DataWritersStatsEngine, StatsEngine }
import io.gatling.core.stats.writer.RunMessage

import akka.actor.ActorSystem
import akka.pattern.ask
import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.EventLoopGroup

private object Runner {

  def apply(system: ActorSystem, eventLoopGroup: EventLoopGroup, configuration: GatlingConfiguration): Runner =
    new Runner(system, eventLoopGroup, new DefaultClock, configuration)
}

private[gatling] class Runner(system: ActorSystem, eventLoopGroup: EventLoopGroup, clock: Clock, configuration: GatlingConfiguration) extends StrictLogging {

  private[app] def run(selectedSimulationClass: SelectedSimulationClass): RunResult =
    configuration.core.directory.reportsOnly match {
      case Some(runId) => new RunResult(runId, hasAssertions = true)
      case _ =>
        if (configuration.http.enableGA) Ga.send(configuration.core.version)
        run0(selectedSimulationClass)
    }

  protected def newStatsEngine(simulationParams: SimulationParams, runMessage: RunMessage): StatsEngine =
    DataWritersStatsEngine(simulationParams, runMessage, system, clock, configuration)

  private def run0(selectedSimulationClass: SelectedSimulationClass): RunResult = {
    logger.trace("Running")

    // ugly way to pass the configuration to the DSL
    io.gatling.core.Predef._configuration = configuration

    val selection = Selection(selectedSimulationClass, configuration)
    val simulation = selection.simulationClass.getDeclaredConstructor().newInstance()
    logger.trace("Simulation instantiated")
    val simulationParams = simulation.params(configuration)
    logger.trace("Simulation params built")

    simulation.executeBefore()
    logger.trace("Before hooks executed")

    val runMessage = RunMessage(simulationParams.name, selection.simulationId, clock.nowMillis, selection.description, configuration.core.version)
    val statsEngine = newStatsEngine(simulationParams, runMessage)
    val throttler = Throttler(system, simulationParams)
    val injector = Injector(system, eventLoopGroup, statsEngine, clock)
    val controller = system.actorOf(Controller.props(statsEngine, injector, throttler, simulationParams), Controller.ControllerActorName)
    val exit = new Exit(injector, clock)
    val coreComponents = new CoreComponents(system, eventLoopGroup, controller, throttler, statsEngine, clock, exit, configuration)
    logger.trace("CoreComponents instantiated")

    val scenarios = simulationParams.scenarios(coreComponents)

    start(simulationParams, scenarios, coreComponents) match {
      case Failure(t) => throw t
      case _ =>
        simulation.executeAfter()
        logger.trace("After hooks executed")
        new RunResult(runMessage.runId, simulationParams.assertions.nonEmpty)
    }
  }

  protected[gatling] def start(
      simulationParams: SimulationParams,
      scenarios: Scenarios,
      coreComponents: CoreComponents
  ): Try[_] = {
    val timeout = Int.MaxValue.milliseconds - 10.seconds
    val start = coreComponents.clock.nowMillis
    println(s"Simulation ${simulationParams.name} started...")
    logger.trace("Asking Controller to start")
    val whenRunDone: Future[Try[String]] = coreComponents.controller.ask(ControllerCommand.Start(scenarios))(timeout).mapTo[Try[String]]
    val runDone = Await.result(whenRunDone, timeout)
    println(s"Simulation ${simulationParams.name} completed in ${(coreComponents.clock.nowMillis - start) / 1000} seconds")
    runDone
  }
}

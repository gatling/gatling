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

package io.gatling.core.controller.inject

import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.duration._

import io.gatling.commons.util.Clock
import io.gatling.core.actor.{ Actor, ActorRef, Behavior, Cancellable, Effect }
import io.gatling.core.controller.Controller
import io.gatling.core.controller.inject.open.OpenInjection
import io.gatling.core.scenario.Population
import io.gatling.core.stats.StatsEngine

import io.netty.channel.EventLoopGroup

private[gatling] object Injector {
  private[inject] val TickPeriod: FiniteDuration = 1.second

  def actor(eventLoopGroup: EventLoopGroup, statsEngine: StatsEngine, clock: Clock): Actor[Injector.Command] =
    new Injector(eventLoopGroup, statsEngine, clock)

  private[gatling] sealed trait Command
  object Command {
    private[controller] final case class Start(controller: ActorRef[Controller.Command], populationFlows: PopulationFlows[String, Population]) extends Command
    private[controller] final case class EmptyInjectionComplete(scenario: String) extends Command
    private[core] final case class UserEnd(scenario: String) extends Command
    private[controller] case object Tick extends Command
  }

  private final case class StartedData(
      controller: ActorRef[Controller.Command],
      inProgressInjections: Map[String, Injection],
      readyInjections: List[Population],
      populationFlows: PopulationFlows[String, Population],
      timer: Cancellable
  )
}

private[gatling] final class Injector private (eventLoopGroup: EventLoopGroup, statsEngine: StatsEngine, clock: Clock)
    extends Actor[Injector.Command]("injector") {
  import Injector._

  override def init(): Behavior[Command] = {
    case Command.Start(controller, scenarioFlows) =>
      val timer = scheduler.scheduleAtFixedRate(TickPeriod) {
        self ! Command.Tick
      }
      val (readyPopulations, newScenarioFlows) = scenarioFlows.unblocked

      inject(StartedData(controller, Map.empty, readyPopulations, newScenarioFlows, timer), firstBatch = true)

    case msg => dieOnUnexpected(msg)
  }

  private val userIdGen = new AtomicLong

  private def buildInjections(populations: List[Population]): Map[String, Injection] = {
    val startTime = clock.nowMillis
    populations.map { population =>
      population.scenario.name -> population.injectionProfile.injection(population.scenario, userIdGen, startTime, eventLoopGroup, statsEngine, clock)
    }.toMap
  }

  private def inject(data: StartedData, firstBatch: Boolean): Effect[Command] = {
    val newlyInProgressInjections = buildInjections(data.readyInjections)

    val newInProgressInjections = data.inProgressInjections ++ newlyInProgressInjections

    newInProgressInjections.values.filterNot(_.isEmpty).foreach {
      case injection: OpenInjection if firstBatch => injection.injectBatch(TickPeriod * 2) // inject 1 second ahead
      case injection                              => injection.injectBatch(TickPeriod)
    }

    newlyInProgressInjections.values.foreach { injection =>
      if (injection.isEmpty) {
        if (injection.duration == Duration.Zero) {
          logger.info(s"Scenario ${injection.scenarioName}'s injection profile is empty, triggering possible children")
          self ! Command.EmptyInjectionComplete(injection.scenarioName)
        } else {
          logger.info(
            s"Scenario ${injection.scenarioName}'s injection profile is empty, delaying possible children for ${injection.duration.toSeconds} seconds"
          )
          scheduler.scheduleOnce(injection.duration) {
            self ! Command.EmptyInjectionComplete(injection.scenarioName)
          }
        }
      }
    }

    val (allUsersScheduledInjections, notAllUsersScheduledInjections) = newInProgressInjections.partition(_._2.isAllUsersScheduled)

    val allUsersScheduled = notAllUsersScheduledInjections.isEmpty && data.populationFlows.isEmpty

    if (allUsersScheduled) {
      logger.info("All scenarios have their users scheduled")
      data.timer.cancel()
    }

    if (allUsersScheduled && allUsersScheduledInjections.values.forall(_.isAllUsersStopped)) {
      logger.info("All users are already stopped")
      stopRun(data.controller)
    } else {
      become(
        started(
          data.copy(
            inProgressInjections = newInProgressInjections,
            readyInjections = Nil
          )
        )
      )
    }
  }

  private def started(data: StartedData): Behavior[Command] = {
    case Command.UserEnd(scenario) =>
      logger.debug(s"End user #$scenario")
      statsEngine.logUserEnd(scenario)
      val injection = data.inProgressInjections(scenario)
      injection.endUser()
      if (injection.isAllUsersStopped) {
        logger.info(s"All users of scenario $scenario are stopped")
        onPopulationComplete(scenario, data)
      } else {
        stay
      }

    case Command.EmptyInjectionComplete(scenario) =>
      logger.info(s"Scenario $scenario with empty injection profile is complete")
      onPopulationComplete(scenario, data)

    case Command.Tick =>
      inject(data, firstBatch = false)

    case msg => dropUnexpected(msg)
  }

  private def onPopulationComplete(scenario: String, data: StartedData): Effect[Command] = {
    val newInProgressPopulations = data.inProgressInjections - scenario
    // children scenarios will be started on next tick

    val (newReady, newPopulationFlows) = data.populationFlows.remove(scenario).unblocked
    val newReadyPopulations = data.readyInjections ++ newReady

    if (newInProgressPopulations.isEmpty && newReadyPopulations.isEmpty) {
      stopRun(data.controller)
    } else {
      become(
        started(
          data.copy(
            inProgressInjections = newInProgressPopulations,
            readyInjections = newReadyPopulations,
            populationFlows = newPopulationFlows
          )
        )
      )
    }
  }

  private def stopRun(controller: ActorRef[Controller.Command]): Effect[Command] = {
    logger.info("Stopping")
    controller ! Controller.Command.StopLoadGenerator(Controller.Command.StopLoadGenerator.Reason.Graceful.Completed)
    die
  }
}

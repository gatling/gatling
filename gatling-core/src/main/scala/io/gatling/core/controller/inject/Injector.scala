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
    actor(eventLoopGroup, statsEngine, clock, TickPeriod)

  private[inject] def actor(eventLoopGroup: EventLoopGroup, statsEngine: StatsEngine, clock: Clock, tickPeriod: FiniteDuration): Actor[Injector.Command] =
    new Injector(eventLoopGroup, statsEngine, clock, tickPeriod)

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
      populationFlows: PopulationFlows[String, Population],
      timer: Cancellable
  )
}

private[gatling] final class Injector private (eventLoopGroup: EventLoopGroup, statsEngine: StatsEngine, clock: Clock, tickPeriod: FiniteDuration)
    extends Actor[Injector.Command]("injector") {
  import Injector._

  override def init(): Behavior[Command] = {
    case Command.Start(controller, scenarioFlows) =>
      val timer = scheduler.scheduleAtFixedRate(tickPeriod) {
        self ! Command.Tick
      }
      val (readyPopulations, newScenarioFlows) = scenarioFlows.unblocked

      startInjections(readyPopulations, StartedData(controller, Map.empty, newScenarioFlows, timer))

    case msg => dieOnUnexpected(msg)
  }

  private val userIdGen = new AtomicLong

  private def buildInjections(populations: List[Population]): Map[String, Injection] = {
    val startTime = clock.nowMillis
    populations.map { population =>
      population.scenario.name -> population.injectionProfile.injection(population.scenario, userIdGen, startTime, eventLoopGroup, statsEngine, clock)
    }.toMap
  }

  // start right away instead of waiting for the next tick, so children scenarios start as soon as their parents complete
  private def startInjections(populations: List[Population], data: StartedData): Effect[Command] = {
    val newInjections = buildInjections(populations)

    newInjections.values.foreach {
      case injection if injection.isEmpty =>
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
      case injection: OpenInjection => injection.injectBatch(tickPeriod * 2) // inject one tick ahead
      case injection                => injection.injectBatch(tickPeriod)
    }

    afterInjection(data.copy(inProgressInjections = data.inProgressInjections ++ newInjections))
  }

  private def afterInjection(data: StartedData): Effect[Command] = {
    val allUsersScheduled = data.populationFlows.isEmpty && data.inProgressInjections.values.forall(_.isAllUsersScheduled)

    if (allUsersScheduled) {
      logger.info("All scenarios have their users scheduled")
      data.timer.cancel()
    }

    if (allUsersScheduled && data.inProgressInjections.values.forall(_.isAllUsersStopped)) {
      logger.info("All users are already stopped")
      stopRun(data.controller)
    } else {
      become(started(data))
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
      data.inProgressInjections.values.filterNot(_.isEmpty).foreach(_.injectBatch(tickPeriod))
      afterInjection(data)

    case msg => dropUnexpected(msg)
  }

  private def onPopulationComplete(scenario: String, data: StartedData): Effect[Command] = {
    val newInProgressInjections = data.inProgressInjections - scenario
    val (newReady, newPopulationFlows) = data.populationFlows.remove(scenario).unblocked

    if (newInProgressInjections.isEmpty && newReady.isEmpty) {
      stopRun(data.controller)
    } else {
      startInjections(newReady, data.copy(inProgressInjections = newInProgressInjections, populationFlows = newPopulationFlows))
    }
  }

  private def stopRun(controller: ActorRef[Controller.Command]): Effect[Command] = {
    logger.info("Stopping")
    controller ! Controller.Command.StopLoadGenerator(Controller.Command.StopLoadGenerator.Reason.Graceful.Completed)
    die
  }
}

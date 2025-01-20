/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
import io.gatling.core.controller.inject.open.OpenWorkload
import io.gatling.core.scenario.Scenario
import io.gatling.core.stats.StatsEngine

import io.netty.channel.EventLoopGroup

private[gatling] object Injector {
  private[inject] val TickPeriod: FiniteDuration = 1.second

  def actor(eventLoopGroup: EventLoopGroup, statsEngine: StatsEngine, clock: Clock): Actor[Injector.Command] =
    new Injector(eventLoopGroup, statsEngine, clock)

  private[gatling] sealed trait Command
  object Command {
    private[controller] final case class Start(controller: ActorRef[Controller.Command], scenarioFlows: ScenarioFlows[String, Scenario]) extends Command
    private[controller] final case class EmptyWorkloadComplete(scenario: String) extends Command
    private[core] final case class UserEnd(scenario: String) extends Command
    private[controller] case object Tick extends Command
  }

  private final case class StartedData(
      controller: ActorRef[Controller.Command],
      inProgressWorkloads: Map[String, Workload],
      readyScenarios: List[Scenario],
      flows: ScenarioFlows[String, Scenario],
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
      val (readyScenarios, newScenarioFlows) = scenarioFlows.extractReady

      inject(StartedData(controller, Map.empty, readyScenarios, newScenarioFlows, timer), firstBatch = true)

    case msg => dieOnUnexpected(msg)
  }

  private val userIdGen = new AtomicLong

  private def buildWorkloads(scenarios: List[Scenario]): Map[String, Workload] = {
    val startTime = clock.nowMillis
    scenarios.map { scenario =>
      scenario.name -> scenario.injectionProfile.workload(scenario, userIdGen, startTime, eventLoopGroup, statsEngine, clock)
    }.toMap
  }

  private def inject(data: StartedData, firstBatch: Boolean): Effect[Command] = {
    val newlyInProgressWorkloads = buildWorkloads(data.readyScenarios)

    val newInProgressWorkloads = data.inProgressWorkloads ++ newlyInProgressWorkloads

    newInProgressWorkloads.values.filterNot(_.isEmpty).foreach {
      case workload: OpenWorkload if firstBatch => workload.injectBatch(TickPeriod * 2) // inject 1 second ahead
      case workload                             => workload.injectBatch(TickPeriod)
    }

    newlyInProgressWorkloads.values.foreach { workload =>
      if (workload.isEmpty) {
        if (workload.duration == Duration.Zero) {
          logger.info(s"Scenario ${workload.scenarioName}'s injection profile is empty, triggering possible children")
          self ! Command.EmptyWorkloadComplete(workload.scenarioName)
        } else {
          logger.info(
            s"Scenario ${workload.scenarioName}'s injection profile is empty, delaying possible children for ${workload.duration.toSeconds} seconds"
          )
          scheduler.scheduleOnce(workload.duration) {
            self ! Command.EmptyWorkloadComplete(workload.scenarioName)
          }
        }
      }
    }

    val (finishedInjectingWorkloads, injectingWorkloads) = newInProgressWorkloads.partition(_._2.isAllUsersScheduled)

    val doneInjecting = injectingWorkloads.isEmpty && data.flows.isEmpty

    if (doneInjecting) {
      logger.info("All scenarios have finished injecting")
      data.timer.cancel()
    }

    if (doneInjecting && finishedInjectingWorkloads.values.forall(_.isAllUsersStopped)) {
      logger.info("All workloads are already stopped")
      stopInjector(data.controller)
    } else {
      become(
        started(
          data.copy(
            inProgressWorkloads = newInProgressWorkloads,
            readyScenarios = Nil
          )
        )
      )
    }
  }

  private def started(data: StartedData): Behavior[Command] = {
    case Command.UserEnd(scenario) =>
      logger.debug(s"End user #$scenario")
      statsEngine.logUserEnd(scenario)
      val workload = data.inProgressWorkloads(scenario)
      workload.endUser()
      if (workload.isAllUsersStopped) {
        logger.info(s"All users of scenario $scenario are stopped")
        onWorkloadComplete(scenario, data)
      } else {
        stay
      }

    case Command.EmptyWorkloadComplete(scenario) =>
      logger.info(s"Scenario $scenario with empty injection profile is complete")
      onWorkloadComplete(scenario, data)

    case Command.Tick =>
      inject(data, firstBatch = false)

    case msg => dropUnexpected(msg)
  }

  private def onWorkloadComplete(scenario: String, data: StartedData): Effect[Command] = {
    val newInProgressWorkloads = data.inProgressWorkloads - scenario
    // children scenarios will be started on next tick

    val (newReady, newScenarioFlows) = data.flows.remove(scenario).extractReady
    val newReadyScenarios = data.readyScenarios ++ newReady

    if (newInProgressWorkloads.isEmpty && newReadyScenarios.isEmpty) {
      stopInjector(data.controller)
    } else {
      become(
        started(
          data.copy(
            inProgressWorkloads = newInProgressWorkloads,
            readyScenarios = newReadyScenarios,
            flows = newScenarioFlows
          )
        )
      )
    }
  }

  private def stopInjector(controller: ActorRef[Controller.Command]): Effect[Command] = {
    logger.info("Stopping")
    controller ! Controller.Command.RunTerminated
    die
  }
}

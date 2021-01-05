/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
import io.gatling.core.controller.ControllerCommand.InjectorStopped
import io.gatling.core.controller.inject.open.OpenWorkload
import io.gatling.core.scenario.{ Scenario, Scenarios }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.writer.UserEndMessage

import akka.actor.{ ActorRef, ActorSystem, Props }
import io.netty.channel.EventLoopGroup

sealed trait InjectorCommand
object InjectorCommand {
  final case class Start(controller: ActorRef, scenarios: Scenarios) extends InjectorCommand
  final case class EmptyWorkloadComplete(scenario: String) extends InjectorCommand
  final case object Tick extends InjectorCommand
}

object Injector {

  private val InjectorActorName = "gatling-injector"
  val TickPeriod: FiniteDuration = 1.second

  def apply(system: ActorSystem, eventLoopGroup: EventLoopGroup, statsEngine: StatsEngine, clock: Clock): ActorRef =
    system.actorOf(Props(new Injector(eventLoopGroup, statsEngine, clock)), InjectorActorName)
}

private[inject] class Injector(eventLoopGroup: EventLoopGroup, statsEngine: StatsEngine, clock: Clock) extends InjectorFSM {

  import Injector._
  import InjectorCommand._
  import InjectorData._
  import InjectorState._

  startWith(WaitingToStart, NoData)

  private val userIdGen = new AtomicLong

  private def buildWorkloads(scenarios: List[Scenario]): Map[String, Workload] = {
    val startTime = clock.nowMillis
    scenarios.map { scenario =>
      scenario.name -> scenario.injectionProfile.workload(scenario, userIdGen, startTime, eventLoopGroup, statsEngine, clock)
    }.toMap
  }

  private def inject(data: StartedData, firstBatch: Boolean): State = {

    val newlyInProgressWorkloads = buildWorkloads(data.scheduledForNextSecondScenarios)

    val newInProgressWorkloads = data.inProgressWorkloads ++ newlyInProgressWorkloads

    newInProgressWorkloads.values.filterNot(_.isEmpty).foreach {
      case workload: OpenWorkload if firstBatch => workload.injectBatch(TickPeriod * 2) // inject 1 second ahead
      case workload                             => workload.injectBatch(TickPeriod)
    }

    newlyInProgressWorkloads.values.foreach { workload =>
      if (workload.isEmpty) {
        if (workload.duration == Duration.Zero) {
          logger.info(s"Scenario ${workload.scenarioName}'s injection profile is empty, triggering possible children")
          self ! EmptyWorkloadComplete(workload.scenarioName)
        } else {
          logger.info(
            s"Scenario ${workload.scenarioName}'s injection profile is empty, delaying possible children for ${workload.duration.toSeconds} seconds"
          )
          scheduler.scheduleOnce(workload.duration) {
            self ! EmptyWorkloadComplete(workload.scenarioName)
          }
        }
      }
    }

    val (finishedInjectingWorkloads, injectingWorkloads) = newInProgressWorkloads.partition(_._2.isAllUsersScheduled)

    val newlyFinishedInjectingScenarios = finishedInjectingWorkloads.keySet -- data.finishedInjectingScenarios

    newlyFinishedInjectingScenarios.foreach { scenario =>
      logger.info(s"Scenario $scenario has finished injecting")
    }

    val doneInjecting = injectingWorkloads.isEmpty && data.pendingChildrenScenarios.isEmpty

    if (doneInjecting) {
      logger.info("Injecting is done")
      data.timer.cancel()
    }

    if (doneInjecting && finishedInjectingWorkloads.values.forall(_.isAllUsersStopped)) {
      logger.info("All workloads are already stopped")
      stopInjector(data.controller)
    } else {
      goto(Started) using data.copy(
        inProgressWorkloads = newInProgressWorkloads,
        finishedInjectingScenarios = data.finishedInjectingScenarios ++ newlyFinishedInjectingScenarios,
        scheduledForNextSecondScenarios = Nil
      )
    }
  }

  when(WaitingToStart) { case Event(Start(controller, scenarios), NoData) =>
    val timer = system.scheduler.scheduleAtFixedRate(TickPeriod, TickPeriod, self, Tick)
    inject(StartedData(controller, Map.empty, scenarios.roots, Set.empty, scenarios.children, timer), firstBatch = true)
  }

  private def onWorkloadComplete(scenario: String, data: StartedData): State = {
    val newInProgressWorkloads = data.inProgressWorkloads - scenario
    // children scenarios will be started on next tick
    val newScheduledForNextSecondScenarios = data.scheduledForNextSecondScenarios ++ data.pendingChildrenScenarios.getOrElse(scenario, Nil)

    if (newInProgressWorkloads.isEmpty && newScheduledForNextSecondScenarios.isEmpty) {
      stopInjector(data.controller)
    } else {
      stay() using StartedData(
        controller = data.controller,
        inProgressWorkloads = newInProgressWorkloads,
        scheduledForNextSecondScenarios = newScheduledForNextSecondScenarios,
        finishedInjectingScenarios = data.finishedInjectingScenarios,
        pendingChildrenScenarios = data.pendingChildrenScenarios - scenario,
        timer = data.timer
      )
    }
  }

  when(Started) {
    case Event(userMessage @ UserEndMessage(scenario, _), data: StartedData) =>
      logger.debug(s"End user #$scenario")
      val workload = data.inProgressWorkloads(scenario)
      workload.endUser(userMessage)
      if (workload.isAllUsersStopped) {
        logger.info(s"All users of scenario $scenario are stopped")
        onWorkloadComplete(scenario, data)
      } else {
        stay()
      }

    case Event(EmptyWorkloadComplete(scenario), data: StartedData) =>
      logger.info(s"Scenario $scenario with empty injection profile is complete")
      onWorkloadComplete(scenario, data)

    case Event(Tick, data: StartedData) =>
      inject(data, firstBatch = false)
  }

  private def stopInjector(controller: ActorRef): State = {
    logger.info("Stopping")
    controller ! InjectorStopped
    stop()
  }
}

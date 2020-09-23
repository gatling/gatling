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
  final case object Tick extends InjectorCommand
}

object Injector {

  private val InjectorActorName = "gatling-injector"
  val TickPeriod: FiniteDuration = 1 second

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

    val newInProgressWorkloads = data.inProgressWorkloads ++ buildWorkloads(data.todoScenarios)

    newInProgressWorkloads.values.foreach {
      case workload: OpenWorkload if firstBatch => workload.injectBatch(TickPeriod * 2) // inject 1 second ahead
      case workload                             => workload.injectBatch(TickPeriod)
    }

    val (allScheduledWorkloads, stillInjectingProgressWorkloads) = newInProgressWorkloads.partition(_._2.isAllUsersScheduled)

    allScheduledWorkloads.keys.foreach { scenario =>
      logger.info(s"Scenario $scenario has finished injecting")
    }

    val doneInjecting = stillInjectingProgressWorkloads.isEmpty && data.pendingChildrenScenarios.isEmpty

    if (doneInjecting) {
      logger.info("Injecting is done")
      data.timer.cancel()
    }

    if (doneInjecting && allScheduledWorkloads.values.forall(_.isAllUsersStopped)) {
      logger.info("All workloads are already stopped")
      stopInjector(data.controller)
    } else {
      goto(Started) using data.copy(inProgressWorkloads = newInProgressWorkloads, todoScenarios = Nil)
    }
  }

  when(WaitingToStart) {
    case Event(Start(controller, scenarios), NoData) =>
      val rootWorkloads = buildWorkloads(scenarios.roots)
      val timer = system.scheduler.scheduleWithFixedDelay(TickPeriod, TickPeriod, self, Tick)
      inject(StartedData(controller, rootWorkloads, Nil, scenarios.children, timer), firstBatch = true)
  }

  when(Started) {
    case Event(userMessage @ UserEndMessage(scenario, _), data: StartedData) =>
      logger.debug(s"End user #$scenario")
      val workload = data.inProgressWorkloads(scenario)
      workload.endUser(userMessage)
      if (workload.isAllUsersStopped) {
        logger.info(s"All users of scenario $scenario are stopped")
        val newInProgressWorkloads = data.inProgressWorkloads - scenario
        // children scenarios will be started on next tick
        val newTodoScenarios = data.todoScenarios ++ data.pendingChildrenScenarios.getOrElse(scenario, Nil)

        if (newInProgressWorkloads.isEmpty && newTodoScenarios.isEmpty) {
          stopInjector(data.controller)
        } else {
          stay() using StartedData(
            controller = data.controller,
            inProgressWorkloads = newInProgressWorkloads,
            todoScenarios = newTodoScenarios,
            pendingChildrenScenarios = data.pendingChildrenScenarios - scenario,
            timer = data.timer
          )
        }
      } else {
        stay()
      }

    case Event(Tick, data: StartedData) =>
      inject(data, firstBatch = false)
  }

  private def stopInjector(controller: ActorRef): State = {
    logger.info("Stopping")
    controller ! InjectorStopped
    stop()
  }
}

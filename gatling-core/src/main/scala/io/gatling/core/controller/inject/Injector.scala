/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import scala.collection.breakOut
import scala.concurrent.duration._

import io.gatling.commons.util.Clock
import io.gatling.core.controller.ControllerCommand.InjectorStopped
import io.gatling.core.controller.inject.open.OpenWorkload
import io.gatling.core.scenario.Scenario
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.End
import io.gatling.core.stats.writer.UserMessage

import akka.actor.{ ActorRef, ActorSystem, Props }

sealed trait InjectorCommand
object InjectorCommand {
  case class Start(controller: ActorRef, scenarios: List[Scenario]) extends InjectorCommand
  case object Tick extends InjectorCommand
}

object Injector {

  private val InjectorActorName = "gatling-injector"
  val TickPeriod: FiniteDuration = 1 second

  def apply(system: ActorSystem, statsEngine: StatsEngine, clock: Clock): ActorRef =
    system.actorOf(Props(new Injector(statsEngine, clock)), InjectorActorName)
}

private[inject] class Injector(statsEngine: StatsEngine, clock: Clock) extends InjectorFSM {

  import Injector._
  import InjectorState._
  import InjectorData._
  import InjectorCommand._

  private def inject(data: StartedData, firstBatch: Boolean): State = {
    import data._

    workloads.values.foreach {
      case workload: OpenWorkload if firstBatch => workload.injectBatch(TickPeriod * 2) // inject 1 second ahead
      case workload                             => workload.injectBatch(TickPeriod)
    }

    if (workloads.values.forall(_.isAllUsersScheduled)) {
      logger.info(s"StoppedInjecting")
      timer.cancel()
      val pendingWorkloads = workloads.filterNot { case (_, workload) => workload.isAllUsersStopped }

      if (pendingWorkloads.isEmpty) {
        // all users are already stopped
        stopInjector(controller)
      } else {
        goto(StoppedInjecting) using StoppedInjectingData(controller, pendingWorkloads)
      }
    } else {
      goto(Started) using data
    }
  }

  startWith(WaitingToStart, NoData)

  when(WaitingToStart) {
    case Event(Start(controller, scenarios), NoData) =>
      val userIdGen = new AtomicLong
      val startTime = clock.nowMillis

      val workloads: Map[String, Workload] = scenarios.map { scenario =>
        scenario.name -> scenario.injectionProfile.workload(scenario, userIdGen, startTime, system, statsEngine, clock)
      }(breakOut)

      val timer = system.scheduler.schedule(TickPeriod, TickPeriod, self, Tick)
      inject(StartedData(controller, workloads, timer), firstBatch = true)
  }

  when(Started) {
    case Event(UserMessage(session, End, _), data: StartedData) =>
      import data._
      logger.debug(s"End user #${session.userId}")
      val workload = workloads(session.scenario)
      workload.endUser()
      stay()

    case Event(Tick, data: StartedData) =>
      inject(data, firstBatch = false)
  }

  when(StoppedInjecting) {
    case Event(UserMessage(session, End, _), StoppedInjectingData(controller, workloads)) =>

      val scenario = session.scenario
      val workload = workloads(scenario)
      workload.endUser()
      if (workload.isAllUsersStopped) {
        logger.info(s"All users of scenario $scenario are stopped")
        if (workloads.size == 1) {
          stopInjector(controller)
        } else {
          stay() using StoppedInjectingData(controller, workloads - scenario)
        }
      } else {
        stay()
      }
  }

  private def stopInjector(controller: ActorRef): State = {
    logger.info("Stopping")
    controller ! InjectorStopped
    stop()
  }
}

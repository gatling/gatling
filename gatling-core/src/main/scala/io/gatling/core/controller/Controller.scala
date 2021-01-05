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

package io.gatling.core.controller

import scala.util.{ Failure, Success, Try }

import io.gatling.core.controller.inject.InjectorCommand
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.stats.StatsEngine

import akka.actor.{ ActorRef, ActorSelection, ActorSystem, Props }

object Controller {

  val ControllerActorName = "gatling-controller"

  def props(
      statsEngine: StatsEngine,
      injector: ActorRef,
      throttler: Option[Throttler],
      simulationParams: SimulationParams
  ): Props =
    Props(new Controller(statsEngine, injector, throttler, simulationParams))

  def controllerSelection(system: ActorSystem): ActorSelection =
    system.actorSelection("/user/" + ControllerActorName)
}

class Controller(statsEngine: StatsEngine, injector: ActorRef, throttler: Option[Throttler], simulationParams: SimulationParams) extends ControllerFSM {

  import ControllerCommand._
  import ControllerData._
  import ControllerState._

  val maxDurationTimer = "maxDurationTimer"

  startWith(WaitingToStart, NoData)

  when(WaitingToStart) { case Event(Start(scenarios), NoData) =>
    val initData = InitData(sender(), scenarios)

    simulationParams.maxDuration.foreach { maxDuration =>
      logger.debug("Setting up max duration")
      startSingleTimer(maxDurationTimer, MaxDurationReached(maxDuration), maxDuration)
    }

    throttler.foreach(_.start())
    statsEngine.start()
    injector ! InjectorCommand.Start(self, initData.scenarios)

    goto(Started) using StartedData(initData)
  }

  when(Started) {
    case Event(InjectorStopped, data: StartedData) =>
      logger.info(s"Injector has stopped, initiating graceful stop")
      stopGracefully(data, None)

    case Event(MaxDurationReached(maxDuration), data: StartedData) =>
      logger.info(s"Max duration of $maxDuration reached")
      stopGracefully(data, None)

    case Event(Crash(exception), data: StartedData) =>
      logger.error(s"Simulation crashed", exception)
      cancelTimer(maxDurationTimer)
      stopGracefully(data, Some(exception))

    case Event(Kill, StartedData(initData)) =>
      logger.info("Simulation was killed")
      stop(EndData(initData, None))
  }

  private def stopGracefully(startedData: StartedData, exception: Option[Exception]): State = {
    statsEngine.stop(self, exception)
    goto(WaitingForResourcesToStop) using EndData(startedData.initData, exception)
  }

  private def stop(endData: EndData): State = {
    endData.initData.launcher ! replyToLauncher(endData)
    goto(Stopped) using NoData
  }

  when(WaitingForResourcesToStop) {
    case Event(StatsEngineStopped, data: EndData) =>
      logger.debug("StatsEngine was stopped")
      stop(data)

    case Event(Kill, data: EndData) =>
      logger.error("Kill order received")
      stop(data)

    case Event(message, _) =>
      logger.debug(s"Ignore message $message while waiting for resources to stop")
      stay()
  }

  private def replyToLauncher(endData: EndData): Try[Unit] =
    endData.exception match {
      case Some(exception) => Failure(exception)
      case _               => Success(())
    }

  // -- STEP 4 : Controller has been stopped, all new messages will be discarded -- //

  when(Stopped) { case Event(message, NoData) =>
    logger.debug(s"Ignore message $message since Controller has been stopped")
    stay()
  }

  initialize()
}

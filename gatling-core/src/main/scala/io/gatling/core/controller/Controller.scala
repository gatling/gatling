/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.inject.{ InjectorCommand, Injector }
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.End
import io.gatling.core.stats.writer.UserMessage

import akka.actor.Props

object Controller {

  val ControllerActorName = "gatling-controller"

  def props(statsEngine: StatsEngine, throttler: Throttler, simulationParams: SimulationParams, configuration: GatlingConfiguration) =
    Props(new Controller(statsEngine, throttler, simulationParams, configuration))
}

class Controller(statsEngine: StatsEngine, throttler: Throttler, simulationParams: SimulationParams, configuration: GatlingConfiguration)
    extends ControllerFSM {

  import ControllerState._
  import ControllerData._
  import ControllerCommand._

  val maxDurationTimer = "maxDurationTimer"

  startWith(WaitingToStart, NoData)

  when(WaitingToStart) {
    case Event(Start(scenarios), NoData) =>
      val initData = InitData(sender(), scenarios)

      val injector = Injector(system, self, statsEngine, initData.scenarios)

      simulationParams.maxDuration.foreach { maxDuration =>
        logger.debug("Setting up max duration")
        setTimer(maxDurationTimer, ForceStop(), maxDuration)
      }

      throttler.start()
      statsEngine.start()
      injector ! InjectorCommand.Start

      goto(Started) using StartedData(initData, new UserCounts(0L, 0L))
  }

  when(Started) {
    case Event(UserMessage(session, End, _), startedData: StartedData) =>
      logger.debug(s"End user #${session.userId}")
      startedData.userCounts.completed += 1
      evaluateUserCounts(startedData)

    case Event(InjectionStopped(expectedCount), startedData: StartedData) =>
      logger.info(s"InjectionStopped expectedCount=$expectedCount")
      startedData.userCounts.expected = expectedCount
      evaluateUserCounts(startedData)

    case Event(ForceStop(exception), startedData: StartedData) =>
      logger.info("ForceStop")
      stop(startedData, exception)
  }

  private def evaluateUserCounts(startedData: StartedData): State =
    if (startedData.userCounts.allStopped) {
      logger.info("All users are stopped")
      stop(startedData, None)
    } else {
      stay()
    }

  private def stop(startedData: StartedData, exception: Option[Throwable]): State = {
    cancelTimer(maxDurationTimer)
    exception match {
      case None    => logger.info("Asking StatsEngine to stop")
      case Some(e) => logger.error("Asking StatsEngine to stop", e)
    }
    statsEngine.stop(self)
    goto(WaitingForResourcesToStop) using EndData(startedData.initData, exception)
  }

  // -- STEP 3 : Waiting for StatsEngine to stop, discarding all other messages -- //

  when(WaitingForResourcesToStop) {
    case Event(StatsEngineStopped, endData: EndData) =>
      logger.info("StatsEngineStopped")
      endData.initData.launcher ! replyToLauncher(endData)
      goto(Stopped) using NoData

    case Event(message, _) =>
      logger.debug(s"Ignore message $message while waiting for StatsEngine to stop")
      stay()
  }

  private def replyToLauncher(endData: EndData): Try[Unit] =
    endData.exception match {
      case Some(exception) => Failure(exception)
      case None            => Success(())
    }

  // -- STEP 4 : Controller has been stopped, all new messages will be discarded -- //

  when(Stopped) {
    case Event(message, NoData) =>
      logger.debug(s"Ignore message $message since Controller has been stopped")
      stay()
  }

  initialize()
}

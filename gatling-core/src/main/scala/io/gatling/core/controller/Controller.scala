/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.inject.{ Injection, Injector }
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

  val injectorPeriod = 1 second

  startWith(WaitingToStart, NoData)

  // -- STEP 1 :  Waiting for the Runner to start the Controller -- //

  when(WaitingToStart) {
    case Event(Run(scenarios), NoData) =>
      val initData = InitData(sender(), scenarios)
      processInitializationResult(initData)
  }

  private def processInitializationResult(initData: InitData): State = {

    val injector = Injector(system, statsEngine, injectorPeriod, initData.scenarios)

    simulationParams.maxDuration.foreach { maxDuration =>
      logger.debug("Setting up max duration")
      setTimer("maxDurationTimer", ForceTermination(), maxDuration)
    }

    throttler.start()
    // inject twice: one period ahead to avoid bumps
    val injection = injector.inject() + injector.inject()
    scheduleNextInjection(injection)

    goto(Running) using new RunData(initData, injector, 0L, injection.count)
  }

  // -- STEP 3 : The Controller and Data Writers are fully initialized, Simulation is now running -- //

  private def scheduleNextInjection(injection: Injection): Unit =
    if (injection.continue)
      setTimer("injection", ScheduleNextInjection, injectorPeriod, false)

  when(Running) {
    case Event(UserMessage(_, End, _), runData: RunData) =>
      processUserMessage(runData)

    case Event(ScheduleNextInjection, runData: RunData) =>
      val injector = runData.injector
      val injection = injector.inject()
      runData.expectedUsersCount += injection.count
      scheduleNextInjection(injection)
      stay()

    case Event(ForceTermination(exception), runData: RunData) =>
      terminateStatsEngineAndWaitForConfirmation(runData.initData, exception)
  }

  private def processUserMessage(runData: RunData): State = {

    runData.completedUsersCount += 1

    if (runData.completedUsersCount == runData.expectedUsersCount)
      terminateStatsEngineAndWaitForConfirmation(runData.initData, None)
    else
      stay()
  }

  private def terminateStatsEngineAndWaitForConfirmation(initData: InitData, exception: Option[Exception]): State = {
    statsEngine.terminate(self)
    goto(WaitingForStatsEngineToTerminate) using EndData(initData, exception)
  }

  // -- STEP 4 : Waiting for StatsEngine to terminate, discarding all other messages -- //

  when(WaitingForStatsEngineToTerminate) {
    case Event(StatsEngineTerminated, endData: EndData) =>
      endData.initData.runner ! replyToRunner(endData)
      goto(Stopped) using NoData

    case Event(message, _) =>
      logger.debug(s"Ignore message $message while waiting for StatsEngine to terminate")
      stay()
  }

  private def replyToRunner(endData: EndData): Try[Unit] =
    endData.exception match {
      case Some(exception) => Failure(exception)
      case None            => Success(())
    }

  // -- STEP 5 : Controller has been stopped, all new messages will be discarded -- //

  when(Stopped) {
    case Event(message, NoData) =>
      logger.debug(s"Ignore message $message since Controller has been stopped")
      stay()
  }

  initialize()
}

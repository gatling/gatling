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

import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.{ End, Start }
import io.gatling.core.stats.writer.UserMessage
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.TimeHelper.nowMillis

import akka.actor.Props
import com.typesafe.scalalogging.StrictLogging

object Controller extends StrictLogging {

  val ControllerActorName = "gatling-controller"

  def props(statsEngine: StatsEngine, throttler: Throttler, simulationParams: SimulationParams, configuration: GatlingConfiguration) =
    Props(new Controller(statsEngine, throttler, simulationParams, configuration))
}

class Controller(statsEngine: StatsEngine, throttler: Throttler, simulationParams: SimulationParams, configuration: GatlingConfiguration)
    extends ControllerFSM {

  startWith(WaitingToStart, NoData)

  // -- STEP 1 :  Waiting for the Runner to start the Controller -- //

  when(WaitingToStart) {
    case Event(Run(scenarios), NoData) =>
      val initData = InitData(sender(), scenarios)
      processInitializationResult(initData)
  }

  // -- STEP 2 : Process injection and start running -- //

  private def processInitializationResult(initData: InitData): State = {
      def buildUserStreams: Map[String, UserStream] =
        initData.scenarios.map(scenario => scenario.name -> UserStream(scenario, scenario.injectionProfile.allUsers)).toMap

      def setUpSimulationMaxDuration(): Unit =
        simulationParams.maxDuration.foreach { maxDuration =>
          logger.debug("Setting up max duration")
          setTimer("maxDurationTimer", ForceTermination(), maxDuration)
        }

      def startUpScenarios(userStreams: Map[String, UserStream]): BatchScheduler = {
        val scheduler = new BatchScheduler(nowMillis, 10 seconds, self)

        logger.debug("Launching All Scenarios")
        userStreams.values.foreach(scheduler.scheduleUserStream(system, _))
        logger.debug("Finished Launching scenarios executions")

        scheduler
      }

    val userStreams = buildUserStreams
    throttler.start()
    val batchScheduler = startUpScenarios(userStreams)
    setUpSimulationMaxDuration()
    goto(Running) using new RunData(initData, userStreams, batchScheduler, 0L, Long.MinValue)
  }

  // -- STEP 3 : The Controller and Data Writers are fully initialized, Simulation is now running -- //

  when(Running) {
    case Event(userMessage: UserMessage, runData: RunData) =>
      processUserMessage(userMessage, runData)

    case Event(ScheduleNextUserBatch(scenarioName), runData: RunData) =>
      scheduleNextBatch(runData, scenarioName)
      stay()

    case Event(ForceTermination(exception), runData: RunData) =>
      terminateStatsEngineAndWaitForConfirmation(runData.initData, exception)
  }

  private def processUserMessage(userMessage: UserMessage, runData: RunData): State = {
      def startNewUser: State = {
        logger.info(s"Start user #${userMessage.session.userId}")
        statsEngine.logUser(userMessage)
        stay()
      }

      def endUserAndTerminateIfLast: State = {
        runData.completedUsersCount += 1
        dispatchUserEndToDataWriter(userMessage)

        if (userMessage.session.last) {
          runData.expectedUsersCount = userMessage.session.userId + 1
        }

        if (runData.completedUsersCount == runData.expectedUsersCount)
          terminateStatsEngineAndWaitForConfirmation(runData.initData, None)
        else
          stay()
      }

    userMessage.event match {
      case Start => startNewUser
      case End   => endUserAndTerminateIfLast
    }
  }

  private def scheduleNextBatch(runData: RunData, scenarioName: String): Unit = {
    val userStream = runData.userStreams(scenarioName)
    logger.info(s"Starting new user batch for $scenarioName")
    runData.scheduler.scheduleUserStream(system, userStream)
  }

  private def dispatchUserEndToDataWriter(userMessage: UserMessage): Unit = {
    logger.info(s"End user #${userMessage.session.userId}")
    statsEngine.logUser(userMessage)
  }

  private def terminateStatsEngineAndWaitForConfirmation(initData: InitData, exception: Option[Exception]): State = {
    statsEngine.terminate(self)
    goto(WaitingForStatsEngineToTerminate) using EndData(initData, exception)
  }

  // -- STEP 4 : Waiting for DataWriters to terminate, discarding all other messages -- //

  when(WaitingForStatsEngineToTerminate) {
    case Event(StatsEngineTerminated, endData: EndData) =>
      endData.initData.runner ! replyToRunner(endData)
      goto(Stopped) using NoData

    case Event(message, _) =>
      logger.debug(s"Ignore message $message while waiting for DataWriter to terminate")
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

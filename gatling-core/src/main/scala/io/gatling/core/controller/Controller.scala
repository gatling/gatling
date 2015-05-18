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

import java.util.UUID.randomUUID

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import com.typesafe.scalalogging.StrictLogging

import akka.actor.Props

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.message.{ End, Start }
import io.gatling.core.result.writer.{ StatsEngine, UserMessage }
import io.gatling.core.runner.Selection
import io.gatling.core.util.TimeHelper.nowMillis

object Controller extends StrictLogging {
  def props(selection: Selection, statsEngine: StatsEngine, configuration: GatlingConfiguration) =
    Props(new Controller(selection, statsEngine, configuration))
}

class Controller(selection: Selection, statsEngine: StatsEngine, configuration: GatlingConfiguration)
    extends ControllerFSM {

  startWith(WaitingToStart, NoData)

  // -- STEP 1 :  Waiting for the Runner to start the Controller -- //

  when(WaitingToStart) {
    case Event(Run(simulationDef), NoData) =>
      val initData = InitData(sender(), simulationDef)
      processInitializationResult(initData)
  }

  // -- STEP 2 : Waiting for DataWriters to be initialized and confirm initialization -- //

  private def processInitializationResult(initData: InitData): State = {
      def buildUserStreams: Map[String, UserStream] = {
        initData.simulationDef.scenarios.foldLeft((Map.empty[String, UserStream], 0)) { (streamsAndOffset, scenario) =>
          val (streams, offset) = streamsAndOffset

          val stream = scenario.injectionProfile.allUsers.zipWithIndex
          val userStream = UserStream(scenario, offset, stream)

          (streams + (scenario.name -> userStream), offset + scenario.injectionProfile.users)
        }._1
      }

      def setUpSimulationMaxDuration(): Unit =
        initData.simulationDef.maxDuration.foreach { maxDuration =>
          logger.debug("Setting up max duration")
          setTimer("maxDurationTimer", ForceTermination(), maxDuration)
        }

      def startUpScenarios(userIdRoot: String, userStreams: Map[String, UserStream]): BatchScheduler = {
        val scheduler = new BatchScheduler(userIdRoot, nowMillis, 10 seconds, self)

        logger.debug("Launching All Scenarios")
        userStreams.values.foreach(scheduler.scheduleUserStream(system, _))
        logger.debug("Finished Launching scenarios executions")

        setUpSimulationMaxDuration()

        scheduler
      }

    val userIdRoot = math.abs(randomUUID.getMostSignificantBits) + "-"
    val userStreams = buildUserStreams
    val batchScheduler = startUpScenarios(userIdRoot, userStreams)
    val totalUsers = initData.simulationDef.scenarios.map(_.injectionProfile.users).sum
    goto(Running) using new RunData(initData, userStreams, batchScheduler, mutable.Map.empty, 0, totalUsers)
  }

  // -- STEP 3 : The Controller and Data Writers are fully initialized, Simulation is now running -- //

  when(Running) {
    case Event(userMessage: UserMessage, runData: RunData) =>
      processUserMessage(userMessage, runData)

    case Event(ScheduleNextUserBatch(scenarioName), runData: RunData) =>
      scheduleNextBatch(runData, scenarioName)
      stay()

    case Event(ForceTermination(exception), runData: RunData) =>
      endAllRemainingUsers(runData)
      terminateDataWritersAndWaitForConfirmation(runData.initData, exception)
  }

  private def processUserMessage(userMessage: UserMessage, runData: RunData): State = {
      def startNewUser: State = {
        runData.activeUsers += (userMessage.session.userId -> userMessage)
        logger.info(s"Start user #${userMessage.session.userId}")
        statsEngine ! userMessage
        stay()
      }

      def endUserAndTerminateIfLast: State = {
        runData.activeUsers -= userMessage.session.userId
        runData.completedUsersCount += 1
        dispatchUserEndToDataWriter(userMessage)
        if (runData.completedUsersCount == runData.totalUsers)
          terminateDataWritersAndWaitForConfirmation(runData.initData, None)
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

  private def endAllRemainingUsers(runData: RunData): Unit = {
    val now = nowMillis
    for (userMessage <- runData.activeUsers.values) {
      dispatchUserEndToDataWriter(userMessage.copy(event = End, date = now))
    }
  }

  private def dispatchUserEndToDataWriter(userMessage: UserMessage): Unit = {
    logger.info(s"End user #${userMessage.session.userId}")
    statsEngine ! userMessage
  }

  private def terminateDataWritersAndWaitForConfirmation(initData: InitData, exception: Option[Exception]): State = {
    statsEngine.terminate(self)
    goto(WaitingForDataWritersToTerminate) using EndData(initData, exception)
  }

  // -- STEP 4 : Waiting for DataWriters to terminate, discarding all other messages -- //

  when(WaitingForDataWritersToTerminate) {
    case Event(DataWritersTerminated, endData: EndData) =>
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

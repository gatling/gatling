/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.controller

import java.util.UUID.randomUUID

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import com.typesafe.scalalogging.StrictLogging

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import akka.util.Timeout

import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.result.message.{ End, Start }
import io.gatling.core.result.writer.{ DataWriter, RunMessage, UserMessage }
import io.gatling.core.runner.Selection
import io.gatling.core.scenario.SimulationDef
import io.gatling.core.util.TimeHelper.nowMillis

object Controller extends AkkaDefaults with StrictLogging {

  private var _instance: Option[ActorRef] = None

  def run(simulation: SimulationDef, selection: Selection)(implicit timeout: Timeout): Future[Try[String]] = {

    val totalNumberOfUsers = simulation.scenarios.map(_.injectionProfile.users).sum
    logger.info(s"Total number of users : $totalNumberOfUsers")

    val controller = actor("gatling-controller")(new Controller(simulation, selection, totalNumberOfUsers))

    _instance = Some(controller)
    system.registerOnTermination(_instance = None)

    (controller ? Run).mapTo[Try[String]]
  }

  def !(message: Any): Unit =
    _instance match {
      case Some(c) => c ! message
      case None    => logger.debug("Controller hasn't been started")
    }
}

class Controller(simulationDef: SimulationDef, selection: Selection, totalNumberOfUsers: Int)
    extends ControllerStateMachine {

  startWith(WaitingToStart, NoData)

  // -- STEP 1 :  Waiting for the Runner to start the Controller -- //

  when(WaitingToStart) {
    case Event(Run, NoData) =>
      val initializationData = initDataWriters()
      goto(WaitingForDataWritersToInit) using initializationData
  }

  private def initDataWriters(): InitData = {
    val runMessage = RunMessage(simulationDef.name, selection.simulationId, nowMillis, selection.description)
    DataWriter.init(simulationDef.assertions, runMessage, simulationDef.scenarios, self)
    InitData(runMessage.runId, sender())
  }

  // -- STEP 2 : Waiting for DataWriters to be initialized and confirm initialization -- //

  when(WaitingForDataWritersToInit) {
    case Event(DataWritersInitialized(result), initData: InitData) =>
      processInitializationResult(result, initData)

    case Event(message, _) =>
      logger.error(s"Shouldn't happen. Ignore message $message while waiting for DataWriter to initialize")
      stay()
  }

  private def processInitializationResult(result: Try[Unit], initData: InitData): State = {
      def buildUserStreams: Map[String, UserStream] = {
        simulationDef.scenarios.foldLeft((Map.empty[String, UserStream], 0)) { (streamsAndOffset, scenario) =>
          val (streams, offset) = streamsAndOffset

          val stream = scenario.injectionProfile.allUsers.zipWithIndex
          val userStream = UserStream(scenario, offset, stream)

          (streams + (scenario.name -> userStream), offset + scenario.injectionProfile.users)
        }._1
      }

      def setUpSimulationMaxDuration(): Unit =
        simulationDef.maxDuration.foreach { maxDuration =>
          logger.debug("Setting up max duration")
          scheduler.scheduleOnce(maxDuration)(self ! ForceTermination())
        }

      def startUpScenarios(userIdRoot: String, userStreams: Map[String, UserStream]): BatchScheduler = {
        val scheduler = new BatchScheduler(userIdRoot, nowMillis, 10 seconds, self)

        logger.debug("Launching All Scenarios")
        userStreams.values.foreach(scheduler.scheduleUserStream)
        logger.debug("Finished Launching scenarios executions")

        setUpSimulationMaxDuration()

        scheduler
      }

    result match {
      case f: Failure[_] =>
        initData.runner ! f
        goto(Stopped)

      case Success(_) =>
        val userIdRoot = math.abs(randomUUID.getMostSignificantBits) + "-"
        val userStreams = buildUserStreams
        val scheduler = startUpScenarios(userIdRoot, userStreams)
        goto(Running) using RunData(initData, userStreams, scheduler, Map.empty, 0)
    }
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
        val newActiveUsers = runData.activeUsers + (userMessage.userId -> userMessage)
        logger.info(s"Start user #${userMessage.userId}")
        DataWriter ! userMessage
        stay() using runData.copy(activeUsers = newActiveUsers)
      }

      def endUserAndTerminateIfLast: State = {
        val newActiveUsers = runData.activeUsers - userMessage.userId
        val newUserCount = runData.completedUsersCount + 1
        dispatchUserEndToDataWriter(userMessage)
        if (newUserCount == totalNumberOfUsers)
          terminateDataWritersAndWaitForConfirmation(runData.initData, None)
        else
          stay() using runData.copy(completedUsersCount = newUserCount, activeUsers = newActiveUsers)
      }

    userMessage.event match {
      case Start => startNewUser
      case End   => endUserAndTerminateIfLast
    }
  }

  private def scheduleNextBatch(runData: RunData, scenarioName: String): Unit = {
    val userStream = runData.userStreams(scenarioName)
    logger.info(s"Starting new user batch for $scenarioName")
    runData.scheduler.scheduleUserStream(userStream)
  }

  private def endAllRemainingUsers(runData: RunData): Unit = {
    val now = nowMillis
    for (userMessage <- runData.activeUsers.values) {
      dispatchUserEndToDataWriter(userMessage.copy(event = End, endDate = now))
    }
  }

  private def dispatchUserEndToDataWriter(userMessage: UserMessage): Unit = {
    logger.info(s"End user #${userMessage.userId}")
    DataWriter ! userMessage
  }

  private def terminateDataWritersAndWaitForConfirmation(initData: InitData, exception: Option[Exception]): State = {
    DataWriter.terminate(self)
    goto(WaitingForDataWritersToTerminate) using EndData(initData, exception)
  }

  // -- STEP 4 : Waiting for DataWriters to terminate, discarding all other messages -- //

  when(WaitingForDataWritersToTerminate) {
    case Event(DataWritersTerminated, endData: EndData) =>
      endData.initData.runner ! answerToRunner(endData)
      goto(Stopped) using NoData

    case Event(message, _) =>
      logger.debug(s"Ignore message $message while waiting for DataWriter to terminate")
      stay()
  }

  private def answerToRunner(endData: EndData): Try[String] =
    endData.exception match {
      case Some(exception) => Failure(exception)
      case None            => Success(endData.initData.runId)
    }

  // -- STEP 5 : Controller has been stopped, all new messages will be discarded -- //

  when(Stopped) {
    case Event(message, NoData) =>
      logger.debug(s"Ignore message $message since Controller has been stopped")
      stay()
  }

  initialize()
}

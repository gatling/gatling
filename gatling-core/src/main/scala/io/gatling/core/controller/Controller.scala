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

import io.gatling.core.runner.Selection
import io.gatling.core.session.Session

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure => SFailure, Success => SSuccess }

import com.typesafe.scalalogging.StrictLogging

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import akka.util.Timeout
import io.gatling.core.akka.{ AkkaDefaults, BaseActor }
import io.gatling.core.result.message.{ End, Start }
import io.gatling.core.result.writer.{ DataWriter, RunMessage, UserMessage }
import io.gatling.core.scenario.{ SimulationDef, Scenario }
import io.gatling.core.util.TimeHelper._

object Controller extends AkkaDefaults with StrictLogging {

  private var _instance: Option[ActorRef] = None

  def run(simulation: SimulationDef, selection: Selection)(implicit timeout: Timeout): Future[Any] = {

    val totalNumberOfUsers = simulation.scenarios.map(_.injectionProfile.users).sum
    logger.info(s"Total number of users : $totalNumberOfUsers")

    val controller = actor("controller")(new Controller(simulation, selection, totalNumberOfUsers))

    _instance = Some(controller)
    system.registerOnTermination(_instance = None)

    controller.ask(Run)(timeout)
  }

  def !(message: Any): Unit =
    _instance match {
      case Some(c) => c ! message
      case None    => logger.debug("Controller hasn't been started")
    }
}

class Controller(simulation: SimulationDef, selection: Selection, totalNumberOfUsers: Int) extends BaseActor {

  var launcher: ActorRef = _
  val activeUsers = mutable.Map.empty[String, UserMessage]
  var finishedUsers = 0
  var runId: String = _

  val uninitialized: Receive = {

    case Run =>
      val runMessage = RunMessage(simulation.name, selection.simulationId, nowMillis, selection.description)
      runId = runMessage.runId

      launcher = sender()

      DataWriter.init(simulation.assertions, runMessage, simulation.scenarios, self)
      context.become(waitingForDataWriterToInit)
  }

  def waitingForDataWriterToInit: Receive = {

    case DataWritersInitialized(result) => result match {
      case f: SFailure[_] => launcher ! f

      case _ =>
        val userIdRoot = math.abs(randomUUID.getMostSignificantBits) + "-"

        val (userStreams, _) = simulation.scenarios.foldLeft((Map.empty[String, UserStream], 0)) { (streamsAndOffset, scenario) =>
          val (streams, offset) = streamsAndOffset

          val stream = scenario.injectionProfile.allUsers.zipWithIndex
          val userStream = UserStream(scenario, offset, stream)

          (streams + (scenario.name -> userStream), offset + scenario.injectionProfile.users)
        }

        val batcher = batchSchedule(userIdRoot, nowMillis, 10 seconds) _
        logger.debug("Launching All Scenarios")
        userStreams.values.foreach(batcher)
        logger.debug("Finished Launching scenarios executions")

        simulation.maxDuration.foreach {
          logger.debug("Setting up max duration")
          scheduler.scheduleOnce(_) {
            self ! ForceTermination(None)
          }
        }

        context.become(initialized(userStreams, batcher))
    }

    case m => logger.error(s"Shouldn't happen. Ignore message $m while waiting for DataWriter to initialize")
  }

  case class UserStream(scenario: Scenario, offset: Int, stream: Iterator[(FiniteDuration, Int)])

  def batchSchedule(userIdRoot: String, start: Long, batchWindow: FiniteDuration)(userStream: UserStream): Unit = {

    val scenario = userStream.scenario
    val stream = userStream.stream

      def startUser(i: Int): Unit = {
        val session = Session(scenarioName = scenario.name,
          userId = userIdRoot + (i + userStream.offset),
          userEnd = scenario.protocols.userEnd)
        // FIXME why not directly session?
        self ! UserMessage(session.scenarioName, session.userId, Start, session.startDate, 0L)
        scenario.entryPoint ! session
      }

    if (stream.hasNext) {
      val batchTimeOffset = (nowMillis - start).millis
      val nextBatchTimeOffset = batchTimeOffset + batchWindow

      var continue = true

      while (stream.hasNext && continue) {

        val (startingTime, index) = stream.next()
        val delay = startingTime - batchTimeOffset
        continue = startingTime < nextBatchTimeOffset

        if (continue && delay <= ZeroMs)
          startUser(index)

        else
          // Reduce the starting time to the millisecond precision to avoid flooding the scheduler
          scheduler.scheduleOnce(toMillisPrecision(delay)) {
            startUser(index)
          }
      }

      // schedule next batch
      if (stream.hasNext) {
        scheduler.scheduleOnce(batchWindow) {
          self ! ScheduleNextUserBatch(scenario.name)
        }
      }
    }
  }

  def initialized(userStreams: Map[String, UserStream], batcher: UserStream => Unit): Receive = {

      def dispatchUserEndToDataWriter(userMessage: UserMessage): Unit = {
        logger.info(s"End user #${userMessage.userId}")
        DataWriter.dispatch(userMessage)
      }

      def becomeTerminating(exception: Option[Exception]): Unit = {
        DataWriter.terminate(self)
        context.become(waitingForDataWriterToTerminate(exception))
      }

    {
      case userMessage @ UserMessage(_, userId, event, _, _) => event match {
        case Start =>
          activeUsers += userId -> userMessage
          logger.info(s"Start user #${userMessage.userId}")
          DataWriter.dispatch(userMessage)

        case End =>
          finishedUsers += 1
          activeUsers -= userId
          dispatchUserEndToDataWriter(userMessage)
          if (finishedUsers == totalNumberOfUsers)
            becomeTerminating(None)
      }

      case ScheduleNextUserBatch(scenarioName) =>
        val userStream = userStreams(scenarioName)
        logger.info(s"Starting new user batch for $scenarioName")
        batcher(userStream)

      case ForceTermination(exception) =>
        // flush all active users
        val now = nowMillis
        for (activeUser <- activeUsers.values) {
          dispatchUserEndToDataWriter(activeUser.copy(event = End, endDate = now))
        }
        becomeTerminating(exception)
    }
  }

  def waitingForDataWriterToTerminate(exception: Option[Exception]): Receive = {
    case DataWritersTerminated(result) =>
      exception match {
        case Some(e) => launcher ! SFailure(e)
        case _       => launcher ! SSuccess(runId)
      }
    case m => logger.debug(s"Ignore message $m while waiting for DataWriter to terminate")
  }

  def receive = uninitialized
}

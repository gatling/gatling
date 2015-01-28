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
package io.gatling.core.result.writer

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.actor.{ Actor, ActorRef, Props }
import akka.util.Timeout
import io.gatling.core.akka.{ AkkaDefaults, BaseActor }
import io.gatling.core.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.controller.{ DataWritersInitialized, DataWritersTerminated }
import io.gatling.core.result.message._
import io.gatling.core.scenario.Scenario
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.util.TimeHelper._

case class InitDataWriter(totalNumberOfUsers: Int)

object DataWriter extends AkkaDefaults {

  implicit val dataWriterTimeOut = Timeout(5 seconds)

  private var _instances: Option[Seq[ActorRef]] = None

  def instances = _instances match {
    case Some(dw) => dw
    case _        => throw new UnsupportedOperationException("DataWriters haven't been initialized")
  }

  def !(message: DataWriterMessage): Unit = instances.foreach(_ ! message)

  def init(assertions: Seq[Assertion], runMessage: RunMessage, scenarios: Seq[Scenario], replyTo: ActorRef): Unit = {

    _instances = {
      val dw = configuration.data.dataWriterClasses.map { className =>
        val clazz = Class.forName(className).asInstanceOf[Class[Actor]]
        system.actorOf(Props(clazz), clazz.getSimpleName)
      }

      system.registerOnTermination(_instances = None)

      Some(dw)
    }

    val shortScenarioDescriptions = scenarios.map(scenario => ShortScenarioDescription(scenario.name, scenario.injectionProfile.users))
    val responses = instances.map(_ ? Init(assertions, runMessage, shortScenarioDescriptions))
    Future.sequence(responses).map(_ => ()).onComplete(replyTo ! DataWritersInitialized(_))
  }

  def terminate(replyTo: ActorRef): Unit = {
    val responses = instances.map(_ ? Terminate)
    Future.sequence(responses).onComplete(_ => replyTo ! DataWritersTerminated)
  }
}

trait Flushable extends DataWriter {

  def onFlush(): Unit

  val receiveFlush: Receive = {
    case Flush => onFlush()
  }

  abstract override def initialized: Receive = receiveFlush orElse super.initialized
}

/**
 * Abstract class for all DataWriters
 *
 * These writers are responsible for writing the logs that will be read to
 * generate the statistics
 */
abstract class DataWriter extends BaseActor {

  def onInitializeDataWriter(assertions: Seq[Assertion], run: RunMessage, scenarios: Seq[ShortScenarioDescription]): Unit

  def onTerminateDataWriter(): Unit

  def uninitialized: Receive = {
    case Init(assertions, runMessage, scenarios) =>
      logger.info("Initializing")
      onInitializeDataWriter(assertions, runMessage, scenarios)
      logger.info("Initialized")
      context.become(initialized)
      sender ! true

    case m: DataWriterMessage => logger.error(s"Can't handle $m when in uninitialized state, discarding")
  }

  def onMessage(message: LoadEventMessage): Unit

  def initialized: Receive = {

    case Terminate => try {
      onTerminateDataWriter()
    } finally {
      context.become(terminated)
      sender ! true
    }

    case message: LoadEventMessage => onMessage(message)
  }

  def terminated: Receive = {
    case m => logger.info(s"Can't handle $m after being flush")
  }

  def receive = uninitialized
}

trait DataWriterClient {

  def logRequestStart(session: Session,
                      requestName: String): Unit =
    DataWriter ! RequestStartMessage(session.scenarioName,
      session.userId,
      session.groupHierarchy,
      requestName,
      nowMillis)

  def logRequestEnd(session: Session,
                    requestName: String,
                    timings: RequestTimings,
                    status: Status,
                    message: Option[String] = None,
                    extraInfo: List[Any] = Nil): Unit =
    DataWriter ! RequestEndMessage(
      session.scenarioName,
      session.userId,
      session.groupHierarchy,
      requestName,
      timings,
      status,
      message,
      extraInfo)

  def logGroupEnd(session: Session,
                  group: GroupBlock,
                  exitDate: Long): Unit =
    DataWriter ! GroupMessage(
      session.scenarioName,
      session.userId,
      group,
      group.hierarchy,
      group.startDate,
      exitDate,
      group.status)
}

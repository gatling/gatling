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
package io.gatling.core.result.writer

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import io.gatling.core.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.DataWritersTerminated
import io.gatling.core.result.message.{ ResponseTimings, Status }
import io.gatling.core.runner.Selection
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.structure.PopulationBuilder
import io.gatling.core.util.TimeHelper._

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.pattern.ask
import akka.util.Timeout

trait StatsEngineFactory {

  def apply(system: ActorSystem,
            populationBuilders: List[PopulationBuilder],
            assertions: Seq[Assertion],
            selection: Selection,
            runMessage: RunMessage)(implicit configuration: GatlingConfiguration): Future[Try[StatsEngine]]
}

class DefaultStatsEngineFactory extends StatsEngineFactory {

  override def apply(system: ActorSystem,
                     populationBuilders: List[PopulationBuilder],
                     assertions: Seq[Assertion],
                     selection: Selection,
                     runMessage: RunMessage)(implicit configuration: GatlingConfiguration): Future[Try[StatsEngine]] = {

    implicit val dataWriterTimeOut = Timeout(5 seconds)

    val writers = configuration.data.dataWriterClasses.map { className =>
      val clazz = Class.forName(className).asInstanceOf[Class[Actor]]
      system.actorOf(Props(clazz), clazz.getName)
    }

    val shortScenarioDescriptions = populationBuilders.map(populationBuilder => ShortScenarioDescription(populationBuilder.scenarioBuilder.name, populationBuilder.injectionProfile.users))

    val responses = writers.map(_ ? Init(configuration, assertions, runMessage, shortScenarioDescriptions))

      def allSucceeded(responses: Seq[Any]): Boolean =
        responses.map {
          case b: Boolean => b
          case _          => false
        }.forall(identity)

    implicit val dispatcher = system.dispatcher

    Future.sequence(responses)
      .map(allSucceeded)
      .map {
        case true  => Success(new DefaultStatsEngine(system, writers))
        case false => Failure(new Exception("DataWriters didn't initialize properly"))
      }
  }
}

trait StatsEngine {

  def logUser(userMessage: UserMessage): Unit

  def logRequest(session: Session, requestName: String): Unit

  def logResponse(session: Session,
                  requestName: String,
                  timings: ResponseTimings,
                  status: Status,
                  responseCode: Option[String],
                  message: Option[String],
                  extraInfo: List[Any] = Nil): Unit

  def logGroupEnd(session: Session,
                  group: GroupBlock,
                  exitDate: Long): Unit

  def logError(session: Session, requestName: String, error: String, date: Long): Unit

  def terminate(replyTo: ActorRef): Unit

  def reportUnbuildableRequest(session: Session, requestName: String, errorMessage: String): Unit =
    logError(session, requestName, s"Failed to build request $requestName: $errorMessage", nowMillis)
}

class DefaultStatsEngine(system: ActorSystem, writers: Seq[ActorRef]) extends StatsEngine {

  implicit val dispatcher = system.dispatcher

  private def dispatch(message: DataWriterMessage): Unit = writers.foreach(_ ! message)

  override def logUser(userMessage: UserMessage): Unit = dispatch(userMessage)

  override def logRequest(session: Session, requestName: String): Unit =
    dispatch(RequestMessage(session.scenario,
      session.userId,
      session.groupHierarchy,
      requestName,
      nowMillis))

  override def logResponse(session: Session,
                           requestName: String,
                           timings: ResponseTimings,
                           status: Status,
                           responseCode: Option[String],
                           message: Option[String],
                           extraInfo: List[Any] = Nil): Unit =
    dispatch(ResponseMessage(
      session.scenario,
      session.userId,
      session.groupHierarchy,
      requestName,
      timings,
      status,
      responseCode,
      message,
      extraInfo))

  override def logGroupEnd(session: Session,
                           group: GroupBlock,
                           exitDate: Long): Unit =
    dispatch(GroupMessage(
      session.scenario,
      session.userId,
      group.hierarchy,
      group.startDate,
      exitDate,
      group.cumulatedResponseTime,
      group.status))

  override def logError(session: Session, requestName: String, error: String, date: Long): Unit = dispatch(ErrorMessage(s"$error ", date))

  override def terminate(replyTo: ActorRef): Unit = {
    implicit val dataWriterTimeOut = Timeout(5 seconds)
    val responses = writers.map(_ ? Terminate)
    Future.sequence(responses).onComplete(_ => replyTo ! DataWritersTerminated)
  }
}

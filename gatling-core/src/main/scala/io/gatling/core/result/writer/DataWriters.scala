/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.result.writer

import akka.actor.{ ActorSystem, Props, Actor, ActorRef }
import akka.pattern.ask
import akka.util.Timeout
import io.gatling.core.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.DataWritersTerminated
import io.gatling.core.result.message.{ KO, Status, RequestTimings }
import io.gatling.core.runner.Selection
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.structure.PopulationBuilder
import io.gatling.core.util.TimeHelper._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Try, Failure, Success }

object DataWriters {

  implicit val DataWriterTimeOut = Timeout(5 seconds)

  def apply(system: ActorSystem,
            populationBuilders: List[PopulationBuilder],
            assertions: Seq[Assertion],
            selection: Selection,
            runMessage: RunMessage)(implicit configuration: GatlingConfiguration): Future[Try[DataWriters]] = {

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
        case true  => Success(new DataWriters(system, writers))
        case false => Failure(new Exception("DataWriters didn't initialize properly"))
      }
  }
}

class DataWriters(system: ActorSystem, writers: Seq[ActorRef]) {

  import DataWriters._

  implicit val dispatcher = system.dispatcher

  def !(message: DataWriterMessage): Unit = writers.foreach(_ ! message)

  def logRequestStart(session: Session,
                      requestName: String): Unit =
    this ! RequestStartMessage(session.scenarioName,
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
    this ! RequestEndMessage(
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
    this ! GroupMessage(
      session.scenarioName,
      session.userId,
      group.hierarchy,
      group.startDate,
      exitDate,
      group.cumulatedResponseTime,
      group.status)

  def terminate(replyTo: ActorRef): Unit = {
    val responses = writers.map(_ ? Terminate)
    Future.sequence(responses).onComplete(_ => replyTo ! DataWritersTerminated)
  }

  def reportUnbuildableRequest(requestName: String, session: Session, errorMessage: String): Unit = {
    val now = nowMillis
    val timings = RequestTimings(now, now, now, now)
    logRequestEnd(session, requestName, timings, KO, Some(errorMessage))
  }
}

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
package io.gatling.core.stats

import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

import io.gatling.commons.stats.Status
import io.gatling.commons.util.TimeHelper._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.ControllerCommand
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.stats.writer._

import akka.actor.{ Props, Actor, ActorRef, ActorSystem }
import akka.pattern.ask
import akka.util.Timeout

trait StatsEngine {

  def start(): Unit

  def stop(replyTo: ActorRef): Unit

  def logUser(userMessage: UserMessage): Unit

  // [fl]
  //
  //
  //
  //
  //
  //
  //
  //
  //
  // [fl]

  def logResponse(
    session:      Session,
    requestName:  String,
    timings:      ResponseTimings,
    status:       Status,
    responseCode: Option[String],
    message:      Option[String],
    extraInfo:    List[Any]       = Nil
  ): Unit

  def logGroupEnd(
    session:       Session,
    group:         GroupBlock,
    exitTimestamp: Long
  ): Unit

  def logCrash(session: Session, requestName: String, error: String): Unit

  def reportUnbuildableRequest(session: Session, requestName: String, errorMessage: String): Unit =
    logCrash(session, requestName, s"Failed to build request $requestName: $errorMessage")
}

object DataWritersStatsEngine {

  def apply(system: ActorSystem, simulationParams: SimulationParams, runMessage: RunMessage, configuration: GatlingConfiguration): DataWritersStatsEngine = {
    implicit val dataWriterTimeOut = Timeout(5 seconds)

    val dataWriters = configuration.data.dataWriters.map { dw =>
      val clazz = Class.forName(dw.className).asInstanceOf[Class[Actor]]
      system.actorOf(Props(clazz), clazz.getName)
    }

    val shortScenarioDescriptions = simulationParams.populationBuilders.map(pb => ShortScenarioDescription(pb.scenarioBuilder.name, pb.injectionProfile.userCount))

    val dataWriterInitResponses = dataWriters.map(_ ? Init(configuration, simulationParams.assertions, runMessage, shortScenarioDescriptions))

    implicit val dispatcher = system.dispatcher

    val statsEngineFuture = Future.sequence(dataWriterInitResponses)
      .map(_.forall(_ == true))
      .map {
        case true => Success(new DataWritersStatsEngine(system, dataWriters))
        case _    => Failure(new Exception("DataWriters didn't initialize properly"))
      }

    Await.result(statsEngineFuture, 5 seconds).get
  }
}

class DataWritersStatsEngine(system: ActorSystem, dataWriters: Seq[ActorRef]) extends StatsEngine {

  private val active = new AtomicBoolean(true)

  override def start(): Unit = {}

  override def stop(replyTo: ActorRef): Unit =
    if (active.getAndSet(false)) {
      implicit val dispatcher = system.dispatcher
      implicit val dataWriterTimeOut = Timeout(5 seconds)
      val responses = dataWriters.map(_ ? Stop)
      Future.sequence(responses).onComplete(_ => replyTo ! ControllerCommand.StatsEngineStopped)
    }

  private def dispatch(message: DataWriterMessage): Unit = if (active.get) dataWriters.foreach(_ ! message)

  override def logUser(userMessage: UserMessage): Unit = dispatch(userMessage)

  // [fl]
  //
  //
  //
  //
  //
  //
  //
  //
  //
  // [fl]

  override def logResponse(
    session:      Session,
    requestName:  String,
    timings:      ResponseTimings,
    status:       Status,
    responseCode: Option[String],
    message:      Option[String],
    extraInfo:    List[Any]       = Nil
  ): Unit =
    dispatch(ResponseMessage(
      session.scenario,
      session.userId,
      session.groupHierarchy,
      requestName,
      timings,
      status,
      responseCode,
      message,
      extraInfo
    ))

  override def logGroupEnd(
    session:       Session,
    group:         GroupBlock,
    exitTimestamp: Long
  ): Unit =
    dispatch(GroupMessage(
      session.scenario,
      session.userId,
      group.hierarchy,
      group.startTimestamp,
      exitTimestamp,
      group.cumulatedResponseTime,
      group.status
    ))

  override def logCrash(session: Session, requestName: String, error: String): Unit =
    dispatch(ErrorMessage(s"$error ", nowMillis))
}

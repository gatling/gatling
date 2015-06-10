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
package io.gatling.core.stats

import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.Future
import scala.concurrent.duration._

import io.gatling.core.controller.StatsEngineStopped
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.stats.message.{ ResponseTimings, Status }
import io.gatling.core.stats.writer._
import io.gatling.core.util.TimeHelper._

import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern.ask
import akka.util.Timeout

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

  def stop(replyTo: ActorRef): Unit

  def reportUnbuildableRequest(session: Session, requestName: String, errorMessage: String): Unit =
    logError(session, requestName, s"Failed to build request $requestName: $errorMessage", nowMillis)
}

class DefaultStatsEngine(system: ActorSystem, dataWriters: Seq[ActorRef]) extends StatsEngine {

  implicit val dispatcher = system.dispatcher

  private val active = new AtomicBoolean(true)

  private def dispatch(message: DataWriterMessage): Unit = if (active.get) dataWriters.foreach(_ ! message)

  override def logUser(userMessage: UserMessage): Unit = dispatch(userMessage)

  override def logRequest(session: Session, requestName: String): Unit = {}

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

  override def stop(replyTo: ActorRef): Unit =
    if (active.getAndSet(false)) {
      implicit val dataWriterTimeOut = Timeout(5 seconds)
      val responses = dataWriters.map(_ ? Stop)
      Future.sequence(responses).onComplete(_ => replyTo ! StatsEngineStopped)
    }
}

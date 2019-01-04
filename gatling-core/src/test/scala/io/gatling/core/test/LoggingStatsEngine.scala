/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.core.test

import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedDeque

import io.gatling.commons.stats.Status
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.writer.UserMessage

import akka.actor.ActorRef

sealed trait StatsEngineMessage

case class LogUser(userMessage: UserMessage) extends StatsEngineMessage

case class LogResponse(session: Session, requestName: String, startTimestamp: Long, endTimestamp: Long, status: Status, responseCode: Option[String], message: Option[String]) extends StatsEngineMessage

case class LogGroupEnd(session: Session, group: GroupBlock, exitTimestamp: Long) extends StatsEngineMessage

case class LogCrash(session: Session, requestName: String, error: String) extends StatsEngineMessage

class LoggingStatsEngine extends StatsEngine {

  private[test] val msgQueue = new ConcurrentLinkedDeque[Any]

  override def start(): Unit = {}

  override def stop(replyTo: ActorRef, exception: Option[Exception]): Unit = {}

  override def logUser(userMessage: UserMessage): Unit =
    msgQueue.addLast(LogUser(userMessage))

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

  override def logResponse(session: Session, requestName: String, startTimestamp: Long, endTimestamp: Long, status: Status, responseCode: Option[String], message: Option[String]): Unit =
    msgQueue.addLast(LogResponse(session, requestName, startTimestamp, endTimestamp, status, responseCode, message))

  override def logGroupEnd(session: Session, group: GroupBlock, exitTimestamp: Long): Unit =
    msgQueue.addLast(LogGroupEnd(session, group, exitTimestamp))

  override def logCrash(session: Session, requestName: String, error: String): Unit =
    msgQueue.addLast(LogCrash(session, requestName, error))
}

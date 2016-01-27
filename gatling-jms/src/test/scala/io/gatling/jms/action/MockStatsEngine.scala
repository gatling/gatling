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
package io.gatling.jms.action

import io.gatling.commons.stats.Status
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.stats.writer.{ UserMessage, GroupMessage, ResponseMessage, DataWriterMessage }

import akka.actor.ActorRef
import com.typesafe.scalalogging.StrictLogging

class MockStatsEngine extends StatsEngine with StrictLogging {

  var dataWriterMsg: List[DataWriterMessage] = List()

  override def start(): Unit = {}

  override def stop(replyTo: ActorRef): Unit = {}

  override def logUser(userMessage: UserMessage): Unit = {}

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
    message:      Option[String]  = None,
    extraInfo:    List[Any]       = Nil
  ): Unit =
    handle(ResponseMessage(
      session.scenario,
      session.userId,
      session.groupHierarchy,
      requestName,
      timings,
      status,
      None,
      message,
      extraInfo
    ))

  override def logGroupEnd(session: Session, group: GroupBlock, exitTimestamp: Long): Unit =
    handle(GroupMessage(session.scenario, session.userId, group.hierarchy, group.startTimestamp, exitTimestamp, group.cumulatedResponseTime, group.status))

  override def logCrash(session: Session, requestName: String, error: String): Unit = {}

  override def reportUnbuildableRequest(session: Session, requestName: String, errorMessage: String): Unit = {}

  private def handle(msg: DataWriterMessage) = {
    dataWriterMsg = msg :: dataWriterMsg
    logger.info(msg.toString)
  }
}

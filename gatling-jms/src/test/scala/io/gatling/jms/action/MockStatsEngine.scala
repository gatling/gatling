/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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
import io.gatling.core.session.GroupBlock
import io.gatling.core.stats.StatsEngine

import akka.actor.ActorRef
import com.typesafe.scalalogging.StrictLogging

object MockStatsEngine {
  sealed trait Message
  object Message {
    final case class Response(
        scenario: String,
        groups: List[String],
        requestName: String,
        startTimestamp: Long,
        endTimestamp: Long,
        status: Status,
        responseCode: Option[String],
        message: Option[String]
    ) extends Message
    final case class GroupEnd(scenario: String, groupBlock: GroupBlock, exitTimestamp: Long) extends Message
  }
}

class MockStatsEngine extends StatsEngine with StrictLogging {
  var messages: List[MockStatsEngine.Message] = Nil

  override def start(): Unit = {}

  override def stop(controller: ActorRef, exception: Option[Exception]): Unit = {}

  override def logUserStart(scenario: String): Unit = {}

  override def logUserEnd(scenario: String): Unit = {}

  override def logResponse(
      scenario: String,
      groups: List[String],
      requestName: String,
      startTimestamp: Long,
      endTimestamp: Long,
      status: Status,
      responseCode: Option[String],
      message: Option[String]
  ): Unit =
    handle(
      MockStatsEngine.Message.Response(
        scenario,
        groups,
        requestName,
        startTimestamp,
        endTimestamp,
        status,
        responseCode,
        message
      )
    )

  override def logGroupEnd(scenario: String, groupBlock: GroupBlock, exitTimestamp: Long): Unit =
    handle(
      MockStatsEngine.Message
        .GroupEnd(scenario, groupBlock, exitTimestamp)
    )

  override def logCrash(scenario: String, groups: List[String], requestName: String, error: String): Unit = {}

  override def reportUnbuildableRequest(scenario: String, groups: List[String], requestName: String, errorMessage: String): Unit = {}

  private def handle(msg: MockStatsEngine.Message): Unit = {
    messages = msg :: messages
    logger.debug(msg.toString)
  }
}

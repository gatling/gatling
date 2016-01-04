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
package io.gatling.core.stats.writer

import io.gatling.commons.stats.Status
import io.gatling.commons.stats.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.stats.message.{ ResponseTimings, MessageEvent }

case class ShortScenarioDescription(name: String, userCount: Int)

case class RunMessage(
    simulationClassName:     String,
    userDefinedSimulationId: Option[String],
    defaultSimulationId:     String,
    start:                   Long,
    runDescription:          String
) {

  val simulationId = userDefinedSimulationId.getOrElse(defaultSimulationId)
  val runId = simulationId + "-" + start
}

sealed trait DataWriterMessage
case class Init(configuration: GatlingConfiguration, assertions: Seq[Assertion], runMessage: RunMessage, scenarios: Seq[ShortScenarioDescription]) extends DataWriterMessage
case object Flush extends DataWriterMessage
case class Crash(cause: String) extends DataWriterMessage
case object Stop extends DataWriterMessage

sealed trait LoadEventMessage extends DataWriterMessage

case class UserMessage(
  session:   Session,
  event:     MessageEvent,
  timestamp: Long
) extends LoadEventMessage

case class ResponseMessage(
  scenario:       String,
  userId:         Long,
  groupHierarchy: List[String],
  name:           String,
  timings:        ResponseTimings,
  status:         Status,
  responseCode:   Option[String],
  message:        Option[String],
  extraInfo:      List[Any]
) extends LoadEventMessage

case class GroupMessage(
    scenario:              String,
    userId:                Long,
    groupHierarchy:        List[String],
    startTimestamp:        Long,
    endTimestamp:          Long,
    cumulatedResponseTime: Int,
    status:                Status
) extends LoadEventMessage {
  val duration = (endTimestamp - startTimestamp).toInt
}

case class ErrorMessage(message: String, date: Long) extends LoadEventMessage

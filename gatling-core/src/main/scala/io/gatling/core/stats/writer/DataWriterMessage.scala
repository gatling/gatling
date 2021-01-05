/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.time.{ Instant, ZoneOffset, ZonedDateTime }
import java.time.format.DateTimeFormatter

import io.gatling.commons.stats.Status
import io.gatling.commons.stats.assertion.Assertion

final case class ShortScenarioDescription(name: String, totalUserCount: Option[Long])

final case class RunMessage(
    simulationClassName: String,
    simulationId: String,
    start: Long,
    runDescription: String,
    gatlingVersion: String
) {

  val runId: String = simulationId + "-" +
    DateTimeFormatter
      .ofPattern("yyyyMMddHHmmssSSS")
      .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneOffset.UTC))
}

sealed trait DataWriterMessage
final case class Init(assertions: Seq[Assertion], runMessage: RunMessage, scenarios: Seq[ShortScenarioDescription]) extends DataWriterMessage
case object Flush extends DataWriterMessage
final case class Crash(cause: String) extends DataWriterMessage
case object Stop extends DataWriterMessage

sealed trait LoadEventMessage extends DataWriterMessage

final case class UserStartMessage(
    scenario: String,
    timestamp: Long
) extends LoadEventMessage

final case class UserEndMessage(
    scenario: String,
    timestamp: Long
) extends LoadEventMessage

final case class ResponseMessage(
    scenario: String,
    groupHierarchy: List[String],
    name: String,
    startTimestamp: Long,
    endTimestamp: Long,
    status: Status,
    responseCode: Option[String],
    message: Option[String]
) extends LoadEventMessage

final case class GroupMessage(
    scenario: String,
    groupHierarchy: List[String],
    startTimestamp: Long,
    endTimestamp: Long,
    cumulatedResponseTime: Int,
    status: Status
) extends LoadEventMessage {
  val duration: Int = (endTimestamp - startTimestamp).toInt
}

final case class ErrorMessage(message: String, date: Long) extends LoadEventMessage

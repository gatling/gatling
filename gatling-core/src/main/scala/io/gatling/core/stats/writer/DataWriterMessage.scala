/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import java.time.{ Instant, ZoneId, ZonedDateTime }
import java.time.format.DateTimeFormatter

import scala.concurrent.Promise

import io.gatling.commons.stats.Status
import io.gatling.commons.stats.assertion.Assertion

private[stats] final case class ShortScenarioDescription(name: String, totalUserCount: Option[Long])

private[gatling] final case class RunMessage(
    simulationClassName: String,
    simulationId: String,
    start: Long,
    runDescription: String,
    gatlingVersion: String,
    zoneId: ZoneId
) {
  val runId: String = simulationId + "-" +
    DateTimeFormatter
      .ofPattern("yyyyMMddHHmmssSSS")
      .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), zoneId))
}

private[gatling] sealed trait DataWriterMessage
private[gatling] object DataWriterMessage {
  final case class Init(assertions: Seq[Assertion], runMessage: RunMessage, scenarios: Seq[ShortScenarioDescription], startPromise: Promise[Unit])
      extends DataWriterMessage
  case object Flush extends DataWriterMessage
  private[stats] final case class Crash(cause: String) extends DataWriterMessage
  private[stats] final case class Stop(stopPromise: Promise[Unit]) extends DataWriterMessage

  sealed trait LoadEvent extends DataWriterMessage
  object LoadEvent {
    final case class UserStart(scenario: String, timestamp: Long) extends LoadEvent

    final case class UserEnd(scenario: String, timestamp: Long) extends LoadEvent

    final case class Response(
        scenario: String,
        groupHierarchy: List[String],
        name: String,
        startTimestamp: Long,
        endTimestamp: Long,
        status: Status,
        responseCode: Option[String],
        message: Option[String]
    ) extends LoadEvent

    final case class Group(
        scenario: String,
        groupHierarchy: List[String],
        startTimestamp: Long,
        endTimestamp: Long,
        cumulatedResponseTime: Int,
        status: Status
    ) extends LoadEvent {
      val duration: Int = (endTimestamp - startTimestamp).toInt
    }

    final case class Error(message: String, date: Long) extends LoadEvent
  }
}

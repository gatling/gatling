/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.datadog

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import io.gatling.core.stats.writer.{ DataWriterData, DataWriterMessage }

import akka.actor.ActorRef

trait DatadogLabels {
  def value: BigDecimal
  def instant: Instant
}
final case class ErrorLabels(instant: Instant, value: BigDecimal) extends DatadogLabels
final case class UserLabels(
                             instant: Instant,
                             scenario: String,
                             value: BigDecimal
                           ) extends DatadogLabels
final case class ResponseLabels(
                                 instant: Instant,
                                 scenario: String,
                                 status: String,
                                 value: BigDecimal
                               ) extends DatadogLabels

final case class DatadogData(
                              metricsSender: ActorRef,
                              runId: String,
                              simulation: String,
                              startedUsers: ConcurrentHashMap[UUID, UserLabels],
                              finishedUsers: ConcurrentHashMap[UUID, UserLabels],
                              requestLatency: ConcurrentHashMap[UUID, ResponseLabels],
                              errorCounter: ConcurrentHashMap[UUID, ErrorLabels]
                            ) extends DataWriterData

object DatadogData {

  def initialise(
                  metricsSender: ActorRef,
                  init: DataWriterMessage.Init
                ): DatadogData =
    DatadogData(
      metricsSender = metricsSender,
      runId = init.runMessage.runId,
      simulation = init.runMessage.simulationId,
      startedUsers = new ConcurrentHashMap[UUID, UserLabels](),
      finishedUsers = new ConcurrentHashMap[UUID, UserLabels](),
      requestLatency = new ConcurrentHashMap[UUID, ResponseLabels](),
      errorCounter = new ConcurrentHashMap[UUID, ErrorLabels]()
    )

  def remove[T <: DatadogLabels](
                                  id: UUID,
                                  map: ConcurrentHashMap[UUID, T]
                                ): Unit =
    map.remove(id)
}

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
import java.util.concurrent.ConcurrentHashMap

import io.gatling.core.stats.writer.{ DataWriterData, DataWriterMessage }

trait DatadogLabels
final case class ErrorLabels(instant: Instant) extends DatadogLabels
final case class UserLabels(instant: Instant, scenario: String) extends DatadogLabels
final case class ResponseLabels(instant: Instant, scenario: String, status: String) extends DatadogLabels

final case class DatadogData(
    simulation: String,
    run: String,
    startedUsers: ConcurrentHashMap[UserLabels, BigDecimal],
    finishedUsers: ConcurrentHashMap[UserLabels, BigDecimal],
    requestLatency: ConcurrentHashMap[ResponseLabels, BigDecimal],
    errorCounter: ConcurrentHashMap[ErrorLabels, BigDecimal]
) extends DataWriterData {
  def flush(): Unit = {
    startedUsers.clear()
    finishedUsers.clear()
    requestLatency.clear()
    errorCounter.clear()
  }
}

object DatadogData {

  def initialise(init: DataWriterMessage.Init): DatadogData =
    DatadogData(
      simulation = init.runMessage.simulationId,
      run = init.runMessage.runId,
      startedUsers = new ConcurrentHashMap[UserLabels, BigDecimal](),
      finishedUsers = new ConcurrentHashMap[UserLabels, BigDecimal](),
      requestLatency = new ConcurrentHashMap[ResponseLabels, BigDecimal](),
      errorCounter = new ConcurrentHashMap[ErrorLabels, BigDecimal]()
    )
}

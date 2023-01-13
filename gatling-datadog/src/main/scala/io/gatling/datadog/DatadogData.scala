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

final case class ErrorLabels(instant: Instant)

final case class UserLabels(instant: Instant, scenario: String)

final case class ResponseLabels(instant: Instant, scenario: String, status: String)

final case class DatadogData(
    simulation: String,
    run: String,
    startedUsers: ConcurrentHashMap[UserLabels, Int],
    finishedUsers: ConcurrentHashMap[UserLabels, Int],
    requestLatency: ConcurrentHashMap[ResponseLabels, Double],
    errorCounter: ConcurrentHashMap[ErrorLabels, Int]
) extends DataWriterData

object DatadogData {

  def initialise(init: DataWriterMessage.Init): DatadogData =
    DatadogData(
      simulation = init.runMessage.simulationId,
      run = init.runMessage.runId,
      startedUsers = new ConcurrentHashMap[UserLabels, Int](),
      finishedUsers = new ConcurrentHashMap[UserLabels, Int](),
      requestLatency = new ConcurrentHashMap[ResponseLabels, Double](),
      errorCounter = new ConcurrentHashMap[ErrorLabels, Int]()
    )
}

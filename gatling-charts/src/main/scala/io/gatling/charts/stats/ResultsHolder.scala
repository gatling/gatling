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
package io.gatling.charts.stats

import io.gatling.charts.stats.buffers._
import io.gatling.core.config.GatlingConfiguration

private[stats] class ResultsHolder(val minTimestamp: Long, val maxTimestamp: Long, val buckets: Array[Int])(implicit configuration: GatlingConfiguration)
    extends GeneralStatsBuffers(math.ceil((maxTimestamp - minTimestamp) / 1000.0).toInt)
    with Buckets
    with RunTimes
    with NamesBuffers
    with RequestsPerSecBuffers
    with ResponseTimeRangeBuffers
    with SessionDeltaPerSecBuffers
    with ResponsesPerSecBuffers
    with ErrorsBuffers
    with RequestPercentilesBuffers
    with GroupPercentilesBuffers {

  def addUserRecord(record: UserRecord): Unit = {
    addSessionBuffers(record)
    addScenarioName(record)
  }

  def addGroupRecord(record: GroupRecord): Unit = {
    addGroupName(record)
    updateGroupGeneralStatsBuffers(record)
    updateGroupPercentilesBuffers(record)
    updateGroupResponseTimeRangeBuffer(record)
  }

  def addRequestRecord(record: RequestRecord): Unit = {
    updateRequestsPerSecBuffers(record)
    updateResponsesPerSecBuffers(record)
    addRequestName(record)
    updateRequestGeneralStatsBuffers(record)
    updateResponseTimeRangeBuffer(record)
    updateErrorBuffers(record)
    updateRequestPercentilesBuffers(record)
  }

  def addErrorRecord(record: ErrorRecord): Unit = {
    updateGlobalError(record.message)
  }
}

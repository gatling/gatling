/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.charts.result.reader

import io.gatling.charts.result.reader.buffers._

class ResultsHolder(minTime: Long,
                    maxTime: Long,
                    requestResponseTimeRange: (Int, Int),
                    groupDurationRange: (Int, Int),
                    groupCumulatedResponseTimeRange: (Int, Int))
    extends GeneralStatsBuffers(
      maxTime - minTime,
      requestResponseTimeRange,
      groupDurationRange,
      groupCumulatedResponseTimeRange)
    with NamesBuffers
    with RequestsPerSecBuffers
    with ResponseTimeRangeBuffers
    with SessionDeltaPerSecBuffers
    with ResponsesPerSecBuffers
    with ErrorsBuffers
    with RequestPercentilesBuffers
    with GroupPercentilesBuffers {

  def addUserRecord(record: UserRecord) {
    addSessionBuffers(record)
    addScenarioName(record)
  }

  def addGroupRecord(record: GroupRecord) {
    addGroupName(record)
    updateGroupGeneralStatsBuffers(record)
    updateGroupPercentilesBuffers(record)
    updateGroupResponseTimeRangeBuffer(record)
  }

  def addRequestRecord(record: RequestRecord) {
    updateRequestsPerSecBuffers(record)
    updateResponsesPerSecBuffers(record)
    addRequestName(record)
    updateRequestGeneralStatsBuffers(record)
    updateResponseTimeRangeBuffer(record)
    updateErrorBuffers(record)
    updateRequestPercentilesBuffers(record)
  }
}

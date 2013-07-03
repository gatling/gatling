/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import io.gatling.charts.result.reader.buffers.{ ErrorsBuffers, GeneralStatsBuffers, LatencyPerSecBuffers, NamesBuffers, RequestsPerSecBuffers, ResponseTimePerSecBuffers, ResponseTimeRangeBuffers, SessionDeltaPerSecBuffers, TransactionsPerSecBuffers }

class ResultsHolder(minTime: Long, maxTime: Long)
	extends GeneralStatsBuffers(maxTime - minTime)
	with LatencyPerSecBuffers
	with NamesBuffers
	with RequestsPerSecBuffers
	with ResponseTimePerSecBuffers
	with ResponseTimeRangeBuffers
	with SessionDeltaPerSecBuffers
	with TransactionsPerSecBuffers
	with ErrorsBuffers {

	def addScenarioRecord(record: ScenarioRecord) {
		addSessionBuffers(record)
		addScenarioName(record)
	}

	def addGroupRecord(record: GroupRecord) {
		addGroupName(record.group, record.startDate)
		updateGroupGeneralStatsBuffers(record.duration, record.group, record.status)
		updateGroupResponseTimePerSecBuffers(record.startDateBucket, record.duration, record.group, record.status)
		updateGroupResponseTimeRangeBuffer(record.duration, record.group, record.status)
	}

	def addRequestRecord(record: RequestRecord) {
		updateRequestsPerSecBuffers(record)
		updateTransactionsPerSecBuffers(record)
		updateResponseTimePerSecBuffers(record)
		updateLatencyPerSecBuffers(record)
		addRequestName(record)
		updateGeneralStatsBuffers(record)
		updateResponseTimeRangeBuffer(record)
		updateErrorBuffers(record)
	}
}

/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.charts.result.reader

import com.excilys.ebi.gatling.charts.result.reader.buffers.{ GeneralStatsBuffers, GroupBuffers, LatencyPerSecBuffers, NamesBuffers, RequestsPerSecBuffers, ResponseTimePerSecBuffers, ResponseTimeRangeBuffers, SessionDeltaPerSecBuffers, TransactionsPerSecBuffers }
import com.excilys.ebi.gatling.core.result.message.RecordEvent.{ END, START }

class ResultsHolder(minTime: Long, maxTime: Long)
	extends GeneralStatsBuffers(maxTime - minTime)
	with LatencyPerSecBuffers
	with NamesBuffers
	with RequestsPerSecBuffers
	with ResponseTimePerSecBuffers
	with ResponseTimeRangeBuffers
	with SessionDeltaPerSecBuffers
	with TransactionsPerSecBuffers
	with GroupBuffers {

	def addScenarioRecord(record: ScenarioRecord) {
		record.event match {
			case START =>
				addStartSessionBuffers(record)
				startGroup(record.user, record.scenario, record.executionDate, None)
				addScenarioName(record)
			case END =>
				addEndSessionBuffers(record)
				endGroup(record.user, record.scenario, record.executionDate)
		}
	}

	def addGroupRecord(record: GroupRecord) {
		record.event match {
			case START =>
				startGroup(record.user, record.scenario, record.executionDate, Some(record.group))
				addGroupName(getCurrentGroup(record.user, record.scenario).get, record.executionDate)
			case END =>
				endGroup(record.user, record.scenario, record.executionDate)
		}
	}

	def addActionRecord(record: ActionRecord) {
		val group = getCurrentGroup(record.user, record.scenario)
		updateRequestsPerSecBuffers(record, group)
		updateTransactionsPerSecBuffers(record, group)
		updateResponseTimePerSecBuffers(record, group)
		updateLatencyPerSecBuffers(record, group)
		addRequestName(record, group)
		updateGeneralStatsBuffers(record, group)
		updateResponseTimeRangeBuffer(record, group)
	}
}

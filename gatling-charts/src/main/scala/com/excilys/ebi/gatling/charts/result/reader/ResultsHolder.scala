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

import com.excilys.ebi.gatling.charts.result.reader.buffers.GeneralStatsBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.GroupBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.LatencyPerSecBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.NamesBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.RequestsPerSecBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.ResponseTimePerSecBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.ResponseTimeRangeBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.SessionDeltaPerSecBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.TransactionsPerSecBuffers
import com.excilys.ebi.gatling.core.result.message.ActionType

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
		record.actionType match {
			case ActionType.START =>
				addStartSessionBuffers(record)
				startGroup(record.user, record.scenario, record.executionDate, None)
			case ActionType.END =>
				addEndSessionBuffers(record)
				endGroup(record.user, record.scenario, record.executionDate)
		}
	}

	def addGroupRecord(record: GroupRecord) {
		record.actionType match {
			case ActionType.START =>
				startGroup(record.user, record.scenario, record.executionDate, Some(record.group))
				addGroupName(record, getCurrentGroup(record.user, record.scenario).get)
			case ActionType.END =>
				endGroup(record.user, record.scenario, record.executionDate)
		}
	}

	def addActionRecord(record: ActionRecord) {
		val group = getCurrentGroup(record.user, record.scenario)
		updateRequestsPerSecBuffers(record, group)
		updateTransactionsPerSecBuffers(record, group)
		updateResponseTimePerSecBuffers(record, group)
		updateLatencyPerSecBuffers(record, group)
		addNames(record, group)
		updateGeneralStatsBuffers(record, group)
		updateResponseTimeRangeBuffer(record, group)
	}
}

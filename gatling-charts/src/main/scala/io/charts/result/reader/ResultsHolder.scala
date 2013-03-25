/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import io.gatling.charts.result.reader.buffers.{ GeneralStatsBuffers, GroupBuffers, LatencyPerSecBuffers, NamesBuffers, RequestsPerSecBuffers, ResponseTimePerSecBuffers, ResponseTimeRangeBuffers, SessionDeltaPerSecBuffers, TransactionsPerSecBuffers }
import io.gatling.core.result.message.{ End, KO, Start }

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
			case Start =>
				addStartSessionBuffers(record)
				addScenarioName(record)
			case End =>
				addEndSessionBuffers(record)
		}
	}

	def addGroupRecord(record: GroupRecord) {
		record.event match {
			case Start =>
				startGroup(record)
				addGroupName(getCurrentGroup(record.user, record.scenario).get, record.executionDate)
			case End =>
				val startEntry = endGroup(record)
				val duration = record.executionDate - startEntry.record.executionDate
				updateGroupGeneralStatsBuffers(duration, startEntry.group, startEntry.status)
				updateGroupResponseTimePerSecBuffers(startEntry.record.executionDateBucket, duration, startEntry.group, startEntry.status)
				updateGroupResponseTimeRangeBuffer(duration, startEntry.group, startEntry.status)
		}
	}

	def addActionRecord(record: ActionRecord) {
		val group = getCurrentGroup(record.user, record.scenario)

		if (group.isDefined && record.status == KO) currentGroupFailed(record.user, record.scenario)

		updateRequestsPerSecBuffers(record, group)
		updateTransactionsPerSecBuffers(record, group)
		updateResponseTimePerSecBuffers(record, group)
		updateLatencyPerSecBuffers(record, group)
		addRequestName(record, group)
		updateGeneralStatsBuffers(record, group)
		updateResponseTimeRangeBuffer(record, group)
	}
}

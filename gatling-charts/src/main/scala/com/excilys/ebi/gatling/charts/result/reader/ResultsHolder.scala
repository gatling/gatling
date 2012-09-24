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

import com.excilys.ebi.gatling.charts.result.reader.buffers.RequestsPerSecBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.TransactionsPerSecBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.ResponseTimePerSecBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.LatencyPerSecBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.ResponseTimeRangeBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.GeneralStatsBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.SessionDeltaPerSecBuffers
import com.excilys.ebi.gatling.charts.result.reader.buffers.NamesBuffers
import com.excilys.ebi.gatling.core.action.EndAction
import com.excilys.ebi.gatling.core.action.StartAction

class ResultsHolder(minTime: Long, maxTime: Long,percentile1: Int,percentile2: Int,lowerBound: Int,higherBound: Int)
	extends GeneralStatsBuffers(maxTime - minTime,percentile1,percentile2)
	with LatencyPerSecBuffers
	with NamesBuffers
	with RequestsPerSecBuffers
	with ResponseTimePerSecBuffers
	with ResponseTimeRangeBuffers
	with SessionDeltaPerSecBuffers
	with TransactionsPerSecBuffers {

	def add(record: ActionRecord) {
		record.request match {
			case StartAction.START_OF_SCENARIO =>
				addStartSessionBuffers(record)
			case EndAction.END_OF_SCENARIO =>
				addEndSessionBuffers(record)
			case _ =>
				updateRequestsPerSecBuffers(record)
				updateTransactionsPerSecBuffers(record)
				updateResponseTimePerSecBuffers(record)
				updateLatencyPerSecBuffers(record)
				addNames(record)
				updateGeneralStatsBuffers(record)
				updateResponseTimeRangeBuffer(record,lowerBound,higherBound)
		}
	}
}

/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.charts.result.reader.stats

import java.util.{ HashMap => JHashMap }

import scala.collection.mutable

import com.excilys.ebi.gatling.charts.result.reader.util.ResultBufferType.{ GLOBAL, ResultBufferType }
import com.excilys.ebi.gatling.core.result.message.RunRecord

import grizzled.slf4j.Logging

class StatsResults extends Logging {
	private val generalStatsBuffer = new JHashMap[ResultBufferType, mutable.Buffer[GeneralStatsRecord]]

	def getGeneralStatsBuffer(bufferType: ResultBufferType) = getBuffer(generalStatsBuffer, bufferType)

	private val latencyPerSecBuffer = new JHashMap[ResultBufferType, mutable.Buffer[LatencyPerSecRecord]]

	def getLatencyPerSecBuffer(bufferType: ResultBufferType) = getBuffer(latencyPerSecBuffer, bufferType)

	private val requestAgainstResponseTimeBuffer = new JHashMap[ResultBufferType, mutable.Buffer[RequestAgainstResponseTimeRecord]]

	def getRequestAgainstResponseTimeBuffer(bufferType: ResultBufferType) = getBuffer(requestAgainstResponseTimeBuffer, bufferType)

	private val requestsPerSecBuffer = new JHashMap[ResultBufferType, mutable.Buffer[RequestsPerSecRecord]]

	def getRequestsPerSecBuffer(bufferType: ResultBufferType) = getBuffer(requestsPerSecBuffer, bufferType)

	private val responseTimeDistributionBuffer = new JHashMap[ResultBufferType, mutable.Buffer[ResponseTimeDistributionRecord]]

	def getResponseTimeDistributionBuffer(bufferType: ResultBufferType) = getBuffer(responseTimeDistributionBuffer, bufferType)

	private val responseTimePerSecBuffer = new JHashMap[ResultBufferType, mutable.Buffer[ResponseTimePerSecRecord]]

	def getResponseTimePerSecBuffer(bufferType: ResultBufferType) = getBuffer(responseTimePerSecBuffer, bufferType)

	private val scenarioBuffer = new JHashMap[ResultBufferType, mutable.Buffer[ScenarioRecord]]

	def getScenarioBuffer(bufferType: ResultBufferType = GLOBAL) = getBuffer(scenarioBuffer, bufferType)

	private val requestBuffer = new JHashMap[ResultBufferType, mutable.Buffer[RequestRecord]]

	def getRequestBuffer(bufferType: ResultBufferType = GLOBAL) = getBuffer(requestBuffer, bufferType)

	private val sessionDeltaBuffer = new JHashMap[ResultBufferType, mutable.Buffer[SessionDeltaRecord]]

	def getSessionDeltaBuffer(bufferType: ResultBufferType) = getBuffer(sessionDeltaBuffer, bufferType)

	private val sessionBuffer = new JHashMap[ResultBufferType, mutable.Buffer[SessionRecord]]

	def getSessionBuffer(bufferType: ResultBufferType) = getBuffer(sessionBuffer, bufferType)

	private val transactionPerSecBuffer = new JHashMap[ResultBufferType, mutable.Buffer[TransactionsPerSecRecord]]

	def getTransactionPerSecBuffer(bufferType: ResultBufferType) = getBuffer(transactionPerSecBuffer, bufferType)

	private val runRecordBuffer = new JHashMap[ResultBufferType, mutable.Buffer[RunRecord]]

	def getRunRecordBuffer(bufferType: ResultBufferType = GLOBAL) = getBuffer(runRecordBuffer, bufferType)

	private def getBuffer[A](bufferMap: JHashMap[ResultBufferType, mutable.Buffer[A]], bufferType: ResultBufferType) = {

		if (bufferMap.containsKey(bufferType)) {
			bufferMap.get(bufferType)
		} else {
			val buffer = mutable.ListBuffer[A]()
			bufferMap.put(bufferType, buffer)
			buffer
		}
	}
}

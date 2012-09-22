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

	private val requestAgainstResponseTimeBuffer = new JHashMap[ResultBufferType, mutable.Buffer[RequestAgainstResponseTimeRecord]]
	private val sessionBuffer = new JHashMap[ResultBufferType, mutable.Buffer[SessionRecord]]
	private val runRecordBuffer = new JHashMap[ResultBufferType, mutable.Buffer[RunRecord]]

	private val responseTimeDistributionBuffer = new JHashMap[ResultBufferType, mutable.Buffer[ResponseTimeDistributionRecord]]
	private val generalStatsBuffer = new JHashMap[ResultBufferType, mutable.Buffer[GeneralStatsRecord]]
	private val scenarioBuffer = new JHashMap[ResultBufferType, mutable.Buffer[ScenarioRecord]]
	private val requestBuffer = new JHashMap[ResultBufferType, mutable.Buffer[RequestRecord]]
	private val sessionDeltaBuffer = new JHashMap[ResultBufferType, mutable.Buffer[SessionDeltaRecord]]
	private val requestsPerSecBuffer = new JHashMap[ResultBufferType, mutable.Buffer[RequestsPerSecRecord]]
	private val transactionPerSecBuffer = new JHashMap[ResultBufferType, mutable.Buffer[TransactionsPerSecRecord]]
	private val responseTimePerSecBuffer = new JHashMap[ResultBufferType, mutable.Buffer[ResponseTimePerSecRecord]]
	private val latencyPerSecBuffer = new JHashMap[ResultBufferType, mutable.Buffer[LatencyPerSecRecord]]

	def getGeneralStatsBuffer(bufferType: ResultBufferType) = getBuffer(generalStatsBuffer, bufferType)
	def getLatencyPerSecBuffer(bufferType: ResultBufferType) = getBuffer(latencyPerSecBuffer, bufferType)
	def getRequestAgainstResponseTimeBuffer(bufferType: ResultBufferType) = getBuffer(requestAgainstResponseTimeBuffer, bufferType)
	def getRequestsPerSecBuffer(bufferType: ResultBufferType) = getBuffer(requestsPerSecBuffer, bufferType)
	def getResponseTimeDistributionBuffer(bufferType: ResultBufferType) = getBuffer(responseTimeDistributionBuffer, bufferType)
	def getResponseTimePerSecBuffer(bufferType: ResultBufferType) = getBuffer(responseTimePerSecBuffer, bufferType)
	def getScenarioBuffer(bufferType: ResultBufferType = GLOBAL) = getBuffer(scenarioBuffer, bufferType)
	def getRequestBuffer(bufferType: ResultBufferType = GLOBAL) = getBuffer(requestBuffer, bufferType)
	def getSessionDeltaBuffer(bufferType: ResultBufferType) = getBuffer(sessionDeltaBuffer, bufferType)
	def getSessionBuffer(bufferType: ResultBufferType) = getBuffer(sessionBuffer, bufferType)
	def getTransactionPerSecBuffer(bufferType: ResultBufferType) = getBuffer(transactionPerSecBuffer, bufferType)
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

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
package com.excilys.ebi.gatling.log.stats

import grizzled.slf4j.Logging
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.log.util.ResultBufferType._
import collection.mutable

object StatsResults extends Logging {
	private val generalStatsBuffer = mutable.Map[ResultBufferType, mutable.Buffer[GeneralStatsRecord]]()

	def getGeneralStatsBuffer(bufferType: ResultBufferType) = getBuffer(generalStatsBuffer, bufferType)

	private val latencyPerSecBuffer = mutable.Map[ResultBufferType, mutable.Buffer[LatencyPerSecRecord]]()

	def getLatencyPerSecBuffer(bufferType: ResultBufferType) = getBuffer(latencyPerSecBuffer, bufferType)

	private val requestAgainstResponseTimeBuffer = mutable.Map[ResultBufferType, mutable.Buffer[RequestAgainstResponseTimeRecord]]()

	def getRequestAgainstResponseTimeBuffer(bufferType: ResultBufferType) = getBuffer(requestAgainstResponseTimeBuffer, bufferType)

	private val requestsPerSecBuffer = mutable.Map[ResultBufferType, mutable.Buffer[RequestsPerSecRecord]]()

	def getRequestsPerSecBuffer(bufferType: ResultBufferType) = getBuffer(requestsPerSecBuffer, bufferType)

	private val responseTimeDistributionBuffer = mutable.Map[ResultBufferType, mutable.Buffer[ResponseTimeDistributionRecord]]()

	def getResponseTimeDistributionBuffer(bufferType: ResultBufferType) = getBuffer(responseTimeDistributionBuffer, bufferType)

	private val responseTimePerSecBuffer = mutable.Map[ResultBufferType, mutable.Buffer[ResponseTimePerSecRecord]]()

	def getResponseTimePerSecBuffer(bufferType: ResultBufferType) = getBuffer(responseTimePerSecBuffer, bufferType)

	private val scenarioBuffer = mutable.Map[ResultBufferType, mutable.Buffer[ScenarioRecord]]()

	def getScenarioBuffer(bufferType: ResultBufferType = GLOBAL) = getBuffer(scenarioBuffer, bufferType)

	private val sessionDeltaBuffer = mutable.Map[ResultBufferType, mutable.Buffer[SessionDeltaRecord]]()

	def getSessionDeltaBuffer(bufferType: ResultBufferType) = getBuffer(sessionDeltaBuffer, bufferType)

	private val sessionBuffer = mutable.Map[ResultBufferType, mutable.Buffer[SessionRecord]]()

	def getSessionBuffer(bufferType: ResultBufferType) = getBuffer(sessionBuffer, bufferType)

	private val transactionPerSecBuffer = mutable.Map[ResultBufferType, mutable.Buffer[TransactionsPerSecRecord]]()

	def getTransactionPerSecBuffer(bufferType: ResultBufferType) = getBuffer(transactionPerSecBuffer, bufferType)

	private val runRecordBuffer = mutable.Map[ResultBufferType, mutable.Buffer[RunRecord]]()

	def getRunRecordBuffer(bufferType: ResultBufferType = GLOBAL) = getBuffer(runRecordBuffer, bufferType)

	private def getBuffer[A](bufferMap: mutable.Map[ResultBufferType, mutable.Buffer[A]], bufferType: ResultBufferType) =
		bufferMap.getOrElseUpdate(bufferType, mutable.ListBuffer[A]())
}

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
package com.excilys.ebi.gatling.log

import com.excilys.ebi.gatling.core.result.reader.DataReader
import grizzled.slf4j.Logging
import com.excilys.ebi.gatling.core.result.message.RequestStatus
import stats.{StatsHelper, StatsResultsHelper, Stats, StatsResults}
import com.excilys.ebi.gatling.log.processors.{SessionProcessor, LogFilePreProcessor}
import com.excilys.ebi.gatling.core.config.GatlingFiles._
import java.util.regex.Pattern
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR_STRING
import runtime.RichInt
import java.io.File
import io.Source
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration

object FileDataReader {
	val TABULATION_PATTERN = Pattern.compile(TABULATION_SEPARATOR_STRING)
	val SIMULATION_FILES_NAME_PATTERN = """.*\.log"""
}

class FileDataReader(runUuid: String, maxPlotPerSerie: RichInt) extends DataReader(runUuid) with Logging {

	val (max, min, step, size, buckets) = {
		val inputFiles = simulationLogDirectory(runUuid, create = false).files.filter(_.jfile.getName.matches(FileDataReader.SIMULATION_FILES_NAME_PATTERN)).map(_.jfile).toSeq

		val (max, min, step, size) = LogFilePreProcessor.getGeneralStats(multipleFileIterator(inputFiles), maxPlotPerSerie.toInt)

		new Stats(min, max, step, size, multipleFileIterator(inputFiles)).run

		val buckets = StatsHelper.bucketsList(min, max, step)

		SessionProcessor.compute(StatsResults.getSessionDeltaBuffer, StatsResults.getSessionBuffer, buckets)

		(max, min, step, size, buckets)
	}

	def runRecord = StatsResultsHelper.getRunRecord

	def requestNames = StatsResultsHelper.getRequestNames

	def scenarioNames = StatsResultsHelper.getScenarioNames

	def numberOfActiveSessionsPerSecond(scenarioName: Option[String]) = StatsResultsHelper.getNumberOfActiveSessionsPerSecond(scenarioName)

	def numberOfRequestsPerSecond(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = StatsResultsHelper.getRequestsPerSec(status, requestName, buckets)

	def numberOfTransactionsPerSecond(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = StatsResultsHelper.getTransactionsPerSec(status, requestName, buckets)

	def responseTimeDistribution(slotsNumber: Int, requestName: Option[String]) = StatsResultsHelper.getResponseTimeDistribution(slotsNumber, requestName)

	def percentiles(percentage1: Double, percentage2: Double, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = StatsResultsHelper.getPercentiles(percentage1, percentage2, status, requestName)

	def minResponseTime(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = StatsResultsHelper.getMinResponseTime(status, requestName)

	def maxResponseTime(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = StatsResultsHelper.getMaxResponseTime(status, requestName)

	def countRequests(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = StatsResultsHelper.getCountRequests(status, requestName)

	def meanResponseTime(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = StatsResultsHelper.getMeanResponseTime(status, requestName)

	def meanLatency(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = StatsResultsHelper.getMeanLatency(status, requestName)

	def meanNumberOfRequestsPerSecond(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = StatsResultsHelper.getMeanNumberOfRequestsPerSecond(status, requestName)

	def responseTimeStandardDeviation(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = StatsResultsHelper.getResponseTimeStandardDeviation(status, requestName)

	def numberOfRequestInResponseTimeRange(lowerBound: Int, higherBound: Int, requestName: Option[String]) = StatsResultsHelper.getNumberOfRequestInResponseTimeRange(lowerBound, higherBound, requestName)

	def responseTimeGroupByExecutionStartDate(status: RequestStatus.RequestStatus, requestName: String) = StatsResultsHelper.getResponseTimeGroupByExecutionStartDate(status, requestName)

	def latencyGroupByExecutionStartDate(status: RequestStatus.RequestStatus, requestName: String) = StatsResultsHelper.getLatencyGroupByExecutionStartDate(status, requestName)

	def requestAgainstResponseTime(status: RequestStatus.RequestStatus, requestName: String) = StatsResultsHelper.getRequestAgainstResponseTime(status, requestName)

	private def multipleFileIterator(files : Seq[File]) = files.map(file => Source.fromFile(file, configuration.encoding).getLines()).reduce((first, second) => first ++ second)
}

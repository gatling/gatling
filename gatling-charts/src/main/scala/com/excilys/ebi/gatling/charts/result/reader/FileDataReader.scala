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
package com.excilys.ebi.gatling.charts.result.reader

import java.io.File
import java.util.regex.Pattern

import scala.io.Source

import com.excilys.ebi.gatling.charts.result.reader.processors.{ PostProcessor, PreProcessor }
import com.excilys.ebi.gatling.charts.result.reader.stats.{ Stats, StatsHelper, StatsResults, StatsResultsHelper }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogDirectory
import com.excilys.ebi.gatling.core.result.message.{ RequestStatus, RunRecord }
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR

import grizzled.slf4j.Logging

object FileDataReader {
	val TABULATION_PATTERN = Pattern.compile(TABULATION_SEPARATOR)
	val SIMULATION_FILES_NAME_PATTERN = """.*\.log"""
}

class FileDataReader(runUuid: String) extends DataReader(runUuid) with Logging {

	val (buckets, results): (IndexedSeq[Long], StatsResults) = {
		val inputFiles = simulationLogDirectory(runUuid, create = false).files.filter(_.jfile.getName.matches(FileDataReader.SIMULATION_FILES_NAME_PATTERN)).map(_.jfile).toSeq

		if (inputFiles.isEmpty) throw new IllegalArgumentException("simulation directory doesn't contain any log file.")

		val (max, min, step, size, runRecords) = PreProcessor.run(multipleFileIterator(inputFiles), configuration.charting.maxPlotsPerSeries)

		val results = Stats.compute(min, max, step, size, multipleFileIterator(inputFiles))

		results.getRunRecordBuffer() ++= runRecords

		val buckets = StatsHelper.bucketsList(min, max, step)

		val completeResults = PostProcessor.run(results, buckets)

		(buckets, completeResults)
	}

	def runRecord: RunRecord = StatsResultsHelper.getRunRecord(results)

	def requestNames: Seq[String] = StatsResultsHelper.getRequestNames(results)

	def scenarioNames: Seq[String] = StatsResultsHelper.getScenarioNames(results)

	def numberOfActiveSessionsPerSecond(scenarioName: Option[String]): Seq[(Long, Long)] = StatsResultsHelper.getNumberOfActiveSessionsPerSecond(results, scenarioName)

	def numberOfRequestsPerSecond(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Seq[(Long, Long)] = StatsResultsHelper.getRequestsPerSec(results, status, requestName, buckets)

	def numberOfTransactionsPerSecond(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Seq[(Long, Long)] = StatsResultsHelper.getTransactionsPerSec(results, status, requestName, buckets)

	def responseTimeDistribution(slotsNumber: Int, requestName: Option[String]): (Seq[(Long, Long)], Seq[(Long, Long)]) = StatsResultsHelper.getResponseTimeDistribution(results, slotsNumber, requestName)

	def percentiles(percentage1: Double, percentage2: Double, status: Option[RequestStatus.RequestStatus], requestName: Option[String]): (Long, Long) = StatsResultsHelper.getPercentiles(results, percentage1, percentage2, status, requestName)

	def minResponseTime(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Long = StatsResultsHelper.getMinResponseTime(results, status, requestName)

	def maxResponseTime(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Long = StatsResultsHelper.getMaxResponseTime(results, status, requestName)

	def countRequests(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Long = StatsResultsHelper.getCountRequests(results, status, requestName)

	def meanResponseTime(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Long = StatsResultsHelper.getMeanResponseTime(results, status, requestName)

	def meanLatency(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Long = StatsResultsHelper.getMeanLatency(results, status, requestName)

	def meanNumberOfRequestsPerSecond(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Long = StatsResultsHelper.getMeanNumberOfRequestsPerSecond(results, status, requestName)

	def responseTimeStandardDeviation(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Long = StatsResultsHelper.getResponseTimeStandardDeviation(results, status, requestName)

	def numberOfRequestInResponseTimeRange(lowerBound: Int, higherBound: Int, requestName: Option[String]): Seq[(String, Long)] = StatsResultsHelper.getNumberOfRequestInResponseTimeRange(results, lowerBound, higherBound, requestName)

	def responseTimeGroupByExecutionStartDate(status: RequestStatus.RequestStatus, requestName: String): Seq[(Long, (Long, Long))] = StatsResultsHelper.getResponseTimeGroupByExecutionStartDate(results, status, requestName)

	def latencyGroupByExecutionStartDate(status: RequestStatus.RequestStatus, requestName: String): Seq[(Long, (Long, Long))] = StatsResultsHelper.getLatencyGroupByExecutionStartDate(results, status, requestName)

	def requestAgainstResponseTime(status: RequestStatus.RequestStatus, requestName: String): Seq[(Long, Long)] = StatsResultsHelper.getRequestAgainstResponseTime(results, status, requestName)

	private def multipleFileIterator(files: Seq[File]): Iterator[String] = files.map(file => Source.fromFile(file, configuration.simulation.encoding).getLines()).reduce((first, second) => first ++ second)
}

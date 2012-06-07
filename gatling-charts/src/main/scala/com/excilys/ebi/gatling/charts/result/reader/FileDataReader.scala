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

import java.util.regex.Pattern

import scala.collection.mutable
import scala.io.Source

import scala.tools.nsc.io.Path



import com.excilys.ebi.gatling.charts.result.reader.FileDataReader.TABULATION_PATTERN
import com.excilys.ebi.gatling.charts.util.StatisticsHelper
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RecordType.{ RUN, ACTION }
import com.excilys.ebi.gatling.core.result.message.RequestStatus.RequestStatus
import com.excilys.ebi.gatling.core.result.message.{ RunRecord, RequestStatus }
import com.excilys.ebi.gatling.core.result.reader.{ DataReader, ChartRequestRecord }
import com.excilys.ebi.gatling.core.util.DateHelper.parseTimestampString
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR_STRING

import grizzled.slf4j.Logging

object FileDataReader {
	val TABULATION_PATTERN = Pattern.compile(TABULATION_SEPARATOR_STRING)
}

class FileDataReader(path: Path) extends DataReader(path) with Logging {

	val (allRunRecords, requestRecords, requestNames, scenarioNames): (Seq[RunRecord], Seq[ChartRequestRecord], Seq[String], Seq[String]) = {

		val runRecords = new mutable.ArrayBuffer[RunRecord]
		val requestRecords = new mutable.ArrayBuffer[ChartRequestRecord]
		val requestNames = new mutable.HashMap[String, Long]
		val scenarioNames = new mutable.HashMap[String, Long]

		(for (line <- Source.fromFile(path.jfile, configuration.encoding).getLines) yield TABULATION_PATTERN.split(line, 0))
			.foreach {
				case Array(RUN, runDate, runId, runDescription) =>
					runRecords += RunRecord(parseTimestampString(runDate), runId.intern, runDescription.trim.intern)
				case Array(ACTION, scenarioName, userId, requestName, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage) =>
					val executionStartDateLong = executionStartDate.toLong
					requestRecords += ChartRequestRecord(scenarioName, userId.toInt, requestName, executionStartDateLong, executionEndDate.toLong, requestSendingEndDate.toLong, responseReceivingStartDate.toLong, RequestStatus.withName(resultStatus))
					if (requestName != START_OF_SCENARIO && requestName != END_OF_SCENARIO) {

						val entryTime = requestNames.getOrElse(requestName, Long.MaxValue)
						if (executionStartDateLong < entryTime)
							requestNames += (requestName -> executionStartDateLong)
					}

					val entryTime = scenarioNames.getOrElse(scenarioName, Long.MaxValue)
					if (executionStartDateLong < entryTime)
						scenarioNames += (scenarioName -> executionStartDateLong)

				case record => logger.warn("Malformed line, skipping it : " + record.toList)
			}

		val sortedRequestNames = requestNames.toSeq.sortBy(_._2).map(_._1)
		val sortedScenarioNames = scenarioNames.toSeq.sortBy(_._2).map(_._1)

		(runRecords, requestRecords, sortedRequestNames, sortedScenarioNames)
	}

	val runRecord = {
		if (allRunRecords.size != 1) warn("Expecting one and only one RunRecord")
		allRunRecords.head
	}

	def numberOfActiveSessionsPerSecond(scenarioName: Option[String] = None): Seq[(Long, Int)] =
		StatisticsHelper.numberOfActiveSessionsPerSecond(requestRecords, scenarioName)

	def numberOfEventsPerSecond(event: ChartRequestRecord => Long, status: Option[RequestStatus] = None, requestName: Option[String] = None): Map[Long, Int] =
		StatisticsHelper.numberOfEventsPerSecond(requestRecords, event, status, requestName)

	def responseTimeDistribution(slotsNumber: Int, requestName: Option[String] = None) =
		StatisticsHelper.responseTimeDistribution(requestRecords, slotsNumber, requestName)

	def percentiles(percentage1: Double, percentage2: Double, status: Option[RequestStatus] = None, requestName: Option[String] = None): (Long, Long) =
		StatisticsHelper.percentiles(requestRecords, percentage1, percentage2, status, requestName)

	def countRequests(status: Option[RequestStatus] = None, requestName: Option[String] = None): Int =
		StatisticsHelper.countRequests(requestRecords, status, requestName)

	def minResponseTime(status: Option[RequestStatus] = None, requestName: Option[String] = None): Long =
		StatisticsHelper.minResponseTime(requestRecords, status, requestName)

	def maxResponseTime(status: Option[RequestStatus] = None, requestName: Option[String] = None): Long =
		StatisticsHelper.maxResponseTime(requestRecords, status, requestName)

	def meanResponseTime(status: Option[RequestStatus], requestName: Option[String]): Long =
		StatisticsHelper.meanResponseTime(requestRecords, status, requestName)

	def meanLatency(status: Option[RequestStatus], requestName: Option[String]): Long =
		StatisticsHelper.meanLatency(requestRecords, status, requestName)

	def responseTimeStandardDeviation(status: Option[RequestStatus] = None, requestName: Option[String] = None): Long =
		StatisticsHelper.responseTimeStandardDeviation(requestRecords, status, requestName): Long

	def numberOfRequestInResponseTimeRange(lowerBound: Int, higherBound: Int, requestName: Option[String] = None): Seq[(String, Int)] =
		StatisticsHelper.numberOfRequestInResponseTimeRange(requestRecords, lowerBound, higherBound, requestName)

	def requestRecordsGroupByExecutionStartDate(requestName: String): Seq[(Long, Seq[ChartRequestRecord])] =
		StatisticsHelper.requestRecordsGroupByExecutionStartDate(requestRecords, requestName)
}

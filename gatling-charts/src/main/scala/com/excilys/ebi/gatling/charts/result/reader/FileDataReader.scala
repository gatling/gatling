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

import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.io.Source
import scala.util.Sorting

import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogFile
import com.excilys.ebi.gatling.core.result.message.RecordType.{ RUN, ACTION }
import com.excilys.ebi.gatling.core.result.message.{ RunRecord, RequestStatus }
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.util.DateHelper.parseTimestampString
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR_STRING
import com.excilys.ebi.gatling.charts.result.reader.FileDataReader.TABULATION_PATTERN
import com.excilys.ebi.gatling.core.result.reader.ChartRequestRecord

import grizzled.slf4j.Logging

object FileDataReader {
	val TABULATION_PATTERN = Pattern.compile(TABULATION_SEPARATOR_STRING)
}

class FileDataReader(runUuid: String) extends DataReader(runUuid) with Logging {

	val (allRunRecords, allChartRequestRecords, requestNames, scenarioNames): (Seq[RunRecord], Seq[ChartRequestRecord], Seq[String], Seq[String]) = {

		val runRecords = new mutable.ArrayBuffer[RunRecord]
		val records = new mutable.ArrayBuffer[ChartRequestRecord]
		val requestNames = new mutable.HashSet[String]
		val scenarioNames = new mutable.HashSet[String]

		(for (line <- Source.fromFile(simulationLogFile(runUuid).jfile, configuration.encoding).getLines) yield TABULATION_PATTERN.split(line, 0))
			.foreach {
				case Array(RUN, runDate, runId, runDescription) =>
					runRecords += RunRecord(parseTimestampString(runDate), runId.intern, runDescription.trim.intern)
				case Array(ACTION, scenarioName, userId, requestName, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage) =>
					records += ChartRequestRecord(scenarioName, userId.toInt, requestName, executionStartDate.toLong, executionEndDate.toLong, requestSendingEndDate.toLong, responseReceivingStartDate.toLong, RequestStatus.withName(resultStatus))
					if (requestName != START_OF_SCENARIO && requestName != END_OF_SCENARIO)
						requestNames += requestName
					scenarioNames += scenarioName
				case record => logger.warn("Malformed line, skipping it : " + record.toList)
			}

		implicit val ordering = Ordering.by((_: ChartRequestRecord).executionStartDateNoMillis)
		val sortedRecords = Sorting.stableSort(records)

		(runRecords, sortedRecords.toSeq, requestNames.toSeq, scenarioNames.toSeq)
	}

	val realChartRequestRecords = allChartRequestRecords.filter(record => record.requestName != START_OF_SCENARIO && record.requestName != END_OF_SCENARIO)

	val runRecord = {
		if (allRunRecords.size != 1) warn("Expecting one and only one RunRecord")
		allRunRecords.head
	}

	def requestRecordsGroupByExecutionStartDateInSeconds: SortedMap[Long, Seq[ChartRequestRecord]] = SortedMap(
		allChartRequestRecords
			.groupBy(_.executionStartDateNoMillis)
			.toSeq: _*)

	def scenarioChartRequestRecordsGroupByExecutionStartDateInSeconds(scenarioName: String): SortedMap[Long, Seq[ChartRequestRecord]] = SortedMap(
		allChartRequestRecords
			.filter(_.scenarioName == scenarioName)
			.groupBy(_.executionStartDateNoMillis)
			.toSeq: _*)

	def realChartRequestRecordsGroupByExecutionStartDateInSeconds: SortedMap[Long, Seq[ChartRequestRecord]] = SortedMap(
		realChartRequestRecords
			.groupBy(_.executionStartDateNoMillis)
			.toSeq: _*)

	def realChartRequestRecordsGroupByExecutionEndDateInSeconds: SortedMap[Long, Seq[ChartRequestRecord]] = SortedMap(
		realChartRequestRecords
			.groupBy(_.executionEndDateNoMillis)
			.toSeq: _*)

	def requestRecords(requestName: String): Seq[ChartRequestRecord] = realChartRequestRecords.filter(_.requestName == requestName)

	def requestRecordsGroupByExecutionStartDate(requestName: String): SortedMap[Long, Seq[ChartRequestRecord]] = SortedMap(
		requestRecords(requestName)
			.groupBy(_.executionStartDateNoMillis)
			.toSeq: _*)

	def requestRecordsGroupByExecutionStartDateInSeconds(requestName: String): SortedMap[Long, Seq[ChartRequestRecord]] = SortedMap(
		requestRecords(requestName)
			.groupBy(_.executionStartDateNoMillis)
			.toSeq: _*)
}
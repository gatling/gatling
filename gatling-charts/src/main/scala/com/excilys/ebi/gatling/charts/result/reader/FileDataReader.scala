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
import scala.collection.mutable.ListBuffer
import scala.io.Source

import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogFile
import com.excilys.ebi.gatling.core.result.message.RecordType.{ RUN, ACTION }
import com.excilys.ebi.gatling.core.result.message.{ RequestRecord, RunRecord, RequestStatus }
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.util.DateHelper.parseTimestampString
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR_STRING
import com.excilys.ebi.gatling.charts.result.reader.FileDataReader.TABULATION_PATTERN

import grizzled.slf4j.Logging

object FileDataReader {
	val TABULATION_PATTERN = Pattern.compile(TABULATION_SEPARATOR_STRING)
}

class FileDataReader(runUuid: String) extends DataReader(runUuid) with Logging {

	private val (allRunRecords, allRequestRecords): (Seq[RunRecord], Seq[RequestRecord]) = {

		val runRecords = new ListBuffer[RunRecord]
		val records = new ListBuffer[RequestRecord]

		(for (line <- Source.fromFile(simulationLogFile(runUuid).jfile, configuration.encoding).getLines) yield TABULATION_PATTERN.split(line, 0))
			.foreach {
				case Array(RUN, runDate, runId, runDescription) =>
					runRecords += RunRecord(parseTimestampString(runDate), runId.intern, runDescription.trim.intern)
				case Array(ACTION, scenarioName, userId, requestName, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage) =>
					records += RequestRecord(scenarioName.intern, userId.toInt, requestName.intern, executionStartDate.toLong, executionEndDate.toLong, requestSendingEndDate.toLong, responseReceivingStartDate.toLong, RequestStatus.withName(resultStatus), resultMessage.intern)
				case record => logger.warn("Malformed line, skipping it : " + record.toList)
			}

		(runRecords, records.sortBy(_.executionStartDate))
	}

	val realRequestRecords = allRequestRecords.filter(record => record.requestName != START_OF_SCENARIO && record.requestName != END_OF_SCENARIO)

	val runRecord = if (allRunRecords.size == 1) allRunRecords.head else throw new IllegalAccessException("Expecting one and only one RunRecord")

	val requestNames: Seq[String] = realRequestRecords.map(_.requestName).distinct

	val scenarioNames: Seq[String] = realRequestRecords.map(_.scenarioName).distinct

	val requestRecordsGroupByExecutionStartDateInSeconds: SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		allRequestRecords
			.groupBy(line => new DateTime(line.executionStartDate).withMillisOfSecond(0).getMillis)
			.toSeq: _*)

	def scenarioRequestRecordsGroupByExecutionStartDateInSeconds(scenarioName: String): SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		allRequestRecords
			.filter(_.scenarioName == scenarioName)
			.groupBy(line => new DateTime(line.executionStartDate).withMillisOfSecond(0).getMillis)
			.toSeq: _*)

	val realRequestRecordsGroupByExecutionStartDateInSeconds: SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		realRequestRecords
			.groupBy(line => new DateTime(line.executionStartDate).withMillisOfSecond(0).getMillis)
			.toSeq: _*)

	val realRequestRecordsGroupByExecutionEndDateInSeconds: SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		realRequestRecords
			.groupBy(record => new DateTime(record.executionStartDate + record.responseTime).withMillisOfSecond(0).getMillis)
			.toSeq: _*)

	def requestRecords(requestName: String): Seq[RequestRecord] = realRequestRecords.filter(_.requestName == requestName)

	def requestRecordsGroupByExecutionStartDate(requestName: String): SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		requestRecords(requestName)
			.groupBy(_.executionStartDate)
			.toSeq: _*)

	def requestRecordsGroupByExecutionStartDateInSeconds(requestName: String): SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		requestRecords(requestName)
			.groupBy(line => new DateTime(line.executionStartDate).withMillisOfSecond(0).getMillis)
			.toSeq: _*)
}
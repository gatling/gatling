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
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import org.joda.time.DateTime
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogFile
import com.excilys.ebi.gatling.core.result.message.RecordType._
import com.excilys.ebi.gatling.core.result.message.RequestRecord
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.util.DateHelper.parseTimestampString
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR_STRING
import FileDataReader.TABULATION_PATTERN
import grizzled.slf4j.Logging
import com.excilys.ebi.gatling.core.result.message.RequestStatus
import scala.collection.mutable.ListBuffer

object FileDataReader {
	val TABULATION_PATTERN = Pattern.compile(TABULATION_SEPARATOR_STRING)
}

class FileDataReader(runUuid: String) extends DataReader(runUuid) with Logging {

	private val runRecords = new ListBuffer[RunRecord]

	private val requestRecords = new ListBuffer[RequestRecord]

	(for (line <- Source.fromFile(simulationLogFile(runUuid).jfile, configuration.encoding).getLines) yield TABULATION_PATTERN.split(line, 0))
		.foreach {
			case Array(RUN, runDate, runId, runDescription) =>
				runRecords + RunRecord(parseTimestampString(runDate), runId, runDescription.trim)
			case Array(ACTION, scenarioName, userId, requestName, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage) =>
				requestRecords + RequestRecord(scenarioName, userId.toInt, requestName, executionStartDate.toLong, executionEndDate.toLong, requestSendingEndDate.toLong, responseReceivingStartDate.toLong, RequestStatus.withName(resultStatus), resultMessage)
			case record => logger.warn("Malformed line, skipping it : " + record.toList)
		}

	private val data: Seq[RequestRecord] = requestRecords
		// filter on time window
		.filter(record => record.executionStartDate >= configuration.chartingTimeWindowLowerBound && record.executionStartDate <= configuration.chartingTimeWindowHigherBound)
		.sortBy(_.executionStartDate)

	private val rawRequestData = data.filter(record => record.requestName != START_OF_SCENARIO && record.requestName != END_OF_SCENARIO)

	val runRecord = if (runRecords.size == 1) runRecords.head else throw new IllegalAccessException("Expecting one and only one RunRecord")

	val requestNames: Seq[String] = rawRequestData
		.map(_.requestName)
		.distinct

	val scenarioNames: Seq[String] = rawRequestData
		.map(_.scenarioName)
		.distinct

	val dataIndexedBySendDateWithoutMillis: SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		data
			.groupBy(line => new DateTime(line.executionStartDate).withMillisOfSecond(0).getMillis)
			.toSeq: _*)

	val requestDataIndexedBySendDateWithoutMillis: SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		rawRequestData
			.groupBy(line => new DateTime(line.executionStartDate).withMillisOfSecond(0).getMillis)
			.toSeq: _*)

	val requestDataIndexedByReceiveDateWithoutMillis: SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		rawRequestData
			.groupBy(record => new DateTime(record.executionStartDate + record.responseTime).withMillisOfSecond(0).getMillis)
			.toSeq: _*)

	def requestData(requestName: String): Seq[RequestRecord] = data.filter(_.requestName == requestName)

	def requestDataIndexedBySendDate(requestName: String): SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		requestData(requestName)
			.groupBy(_.executionStartDate)
			.toSeq: _*)

	def requestDataIndexedBySendDateWithoutMillis(requestName: String): SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		requestData(requestName)
			.groupBy(line => new DateTime(line.executionStartDate).withMillisOfSecond(0).getMillis)
			.toSeq: _*)

	def scenarioDataIndexedBySendDateWithoutMillis(scenarioName: String): SortedMap[Long, Seq[RequestRecord]] = SortedMap(
		data
			.filter(_.scenarioName == scenarioName)
			.groupBy(line => new DateTime(line.executionStartDate).withMillisOfSecond(0).getMillis)
			.toSeq: _*)
}
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
import scala.collection.mutable.{ HashMap, ArrayBuffer }
import scala.io.Source

import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_ENCODING
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogFile
import com.excilys.ebi.gatling.core.config.GatlingConfig
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ResultStatus
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.result.writer.ResultLine
import com.excilys.ebi.gatling.core.util.DateHelper.parseFileNameDateFormat
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR_STRING

import FileDataReader.SPLIT_PATTERN

object FileDataReader {
	val SPLIT_PATTERN = Pattern.compile(TABULATION_SEPARATOR_STRING)
}

class FileDataReader(runOn: String) extends DataReader(runOn) with Logging {

	private val data: Seq[ResultLine] = {

		val lines = Source.fromFile(simulationLogFile(runOn).jfile, CONFIG_ENCODING).getLines

		// check headers correctness
		ResultLine.Headers.check(lines.next)

		def isResultInTimeWindow(result: ResultLine) =
			((!GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_LOWER_BOUND.isDefined
				|| result.executionStartDate >= GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_LOWER_BOUND.get)) &&
				(!GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_HIGHER_BOUND.isDefined
					|| (result.executionStartDate <= GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_HIGHER_BOUND.get))

		(for (line <- lines) yield SPLIT_PATTERN.split(line, 0))
			.filter(strings =>
				if (strings.length == ResultLine.Headers.HEADERS_SEQ.length)
					true
				else {
					// Else, if the resulting data is not well formated print an error message
					logger.warn("simulation.log had bad end of file, statistics will be generated but may not be accurate")
					false
				})
			.map(strings => ResultLine(strings(0), strings(1), strings(2).toInt, strings(3), strings(4).toLong, strings(5).toLong, strings(6).toLong, strings(7).toLong, ResultStatus.withName(strings(8)), strings(9)))
			.toBuffer[ResultLine].sortBy(_.executionStartDate)
	}

	lazy val simulationRunOn: DateTime = parseFileNameDateFormat(data.head.runOn)

	val requestNames: Seq[String] = data.map(_.requestName).distinct.filterNot(value => value == END_OF_SCENARIO || value == START_OF_SCENARIO)

	val scenarioNames: Seq[String] = data.map(_.scenarioName).distinct

	val dataIndexedBySendDateWithoutMillis: SortedMap[Long, Seq[ResultLine]] = SortedMap(data.groupBy(line => new DateTime(line.executionStartDate).withMillisOfSecond(0).getMillis).toSeq: _*)

	val dataIndexedByReceiveDateWithoutMillis: SortedMap[Long, Seq[ResultLine]] = SortedMap(data.groupBy(result => new DateTime(result.executionStartDate + result.responseTime).withMillisOfSecond(0).getMillis).toSeq: _*)

	def requestData(requestName: String): Seq[ResultLine] = data.filter(_.requestName == requestName)

	def scenarioData(scenarioName: String): Seq[ResultLine] = data.filter(_.scenarioName == scenarioName)

	def requestDataIndexedBySendDate(requestName: String): SortedMap[Long, Seq[ResultLine]] = SortedMap(requestData(requestName).groupBy(_.executionStartDate).toSeq: _*)

	def requestDataIndexedBySendDateWithoutMillis(requestName: String): SortedMap[Long, Seq[ResultLine]] = SortedMap(requestData(requestName).groupBy(line => new DateTime(line.executionStartDate).withMillisOfSecond(0).getMillis).toSeq: _*)

	def scenarioDataIndexedBySendDateWithoutMillis(scenarioName: String): SortedMap[Long, Seq[ResultLine]] = SortedMap(scenarioData(scenarioName).groupBy(line => new DateTime(line.executionStartDate).withMillisOfSecond(0).getMillis).toSeq: _*)
}
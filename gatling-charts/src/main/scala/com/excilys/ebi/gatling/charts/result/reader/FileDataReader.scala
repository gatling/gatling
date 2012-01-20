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

import scala.collection.immutable.SortedMap
import scala.collection.mutable.{HashMap, ArrayBuffer}
import scala.io.Source
import org.joda.time.DateTime
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_ENCODING
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogFile
import com.excilys.ebi.gatling.core.config.GatlingConfig
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ResultStatus
import com.excilys.ebi.gatling.core.result.writer.ResultLine
import com.excilys.ebi.gatling.core.util.DateHelper.parseFileNameDateFormat
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR
import com.excilys.ebi.gatling.core.result.reader.DataReader

class FileDataReader(runOn: String) extends DataReader with Logging {

	private val data: Seq[ResultLine] = {

		class Cache[K, V](computeKey: K => K, computeValue: K => V) {
			val map = new HashMap[K, V]

			def get(key: K) = {
				map.getOrElse(key, {
					// don't use getOrUpdate as we don't want to store the whole original String as key
					val newKey = computeKey(key)
					val value = computeValue(newKey)
					map.put(newKey, value)
					value
				})
			}
		}

		class StringKeyCache[V](computeValue: String => V) extends Cache[String, V]((s: String) => new String(s), computeValue)

		// use caches in order to reuse String instances instead of holding multiple references of equal Strings
		val stringCache = new StringKeyCache((s: String) => s)
		val intCache = new StringKeyCache((s: String) => s.toInt)
		val longCache = new StringKeyCache((s: String) => s.toLong)

		val lines = Source.fromFile(simulationLogFile(runOn).jfile, CONFIG_ENCODING).getLines

		// check headers correctness
		ResultLine.Headers.check(lines.next)

		val buffer = new ArrayBuffer[ResultLine]

		def isResultInTimeWindow(result: ResultLine) =
			((!GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_LOWER_BOUND.isDefined
				|| result.executionStartDate >= GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_LOWER_BOUND.get)) &&
				(!GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_HIGHER_BOUND.isDefined
					|| (result.executionStartDate <= GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_HIGHER_BOUND.get))

		for (line <- lines) {
			line.split(TABULATION_SEPARATOR) match {
				// If we have a well formated result
				case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage) => {
					val result = ResultLine(stringCache.get(runOn), stringCache.get(scenarioName), intCache.get(userId), stringCache.get(actionName), longCache.get(executionStartDate), longCache.get(executionEndDate), longCache.get(requestSendingEndDate), longCache.get(responseReceivingStartDate), ResultStatus.withName(resultStatus), stringCache.get(resultMessage))
					if (isResultInTimeWindow(result))
						buffer += result
				}

				// Else, if the resulting data is not well formated print an error message
				case _ => logger.warn("simulation.log had bad end of file, statistics will be generated but may not be accurate")
			}
		}

		buffer.sortBy(_.executionStartDate)
	}

	val simulationRunOn: DateTime = parseFileNameDateFormat(data.head.runOn)

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
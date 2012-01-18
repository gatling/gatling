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
package com.excilys.ebi.gatling.charts.loader

import scala.collection.immutable.SortedMap
import scala.collection.mutable.{ HashMap, ArrayBuffer }
import scala.io.Source
import org.joda.time.DateTime
import com.excilys.ebi.gatling.charts.util.OrderingHelper.DateTimeOrdering
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_ENCODING
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogFile
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ResultStatus
import com.excilys.ebi.gatling.core.result.writer.ResultLine
import com.excilys.ebi.gatling.core.util.DateHelper.{ parseResultDate, parseFileNameDateFormat }
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.core.config.GatlingConfig

class DataLoader(runOn: String) extends Logging {

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
		val dateTimeCache = new StringKeyCache((s: String) => parseResultDate(s))

		val lines = Source.fromFile(simulationLogFile(runOn).jfile, CONFIG_ENCODING).getLines

		// check headers correctness
		ResultLine.Headers.check(lines.next)

		// get time window bounds

		val buffer = new ArrayBuffer[ResultLine]

		def isResultInTimeWindow(result: ResultLine) =
			((!GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_LOWER_BOUND.isDefined
				|| result.executionStartDate.equals(GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_LOWER_BOUND.get)
				|| result.executionStartDate.isAfter(GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_LOWER_BOUND.get)) && (
					!GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_HIGHER_BOUND.isDefined
					|| result.executionStartDate.equals(GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_HIGHER_BOUND.get)
					|| result.executionStartDate.isBefore(GatlingConfig.CONFIG_CHARTING_TIME_WINDOW_HIGHER_BOUND.get)))

		for (line <- lines) {
			line.split(TABULATION_SEPARATOR) match {
				// If we have a well formated result
				case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage) => {
					val result = ResultLine(stringCache.get(runOn), stringCache.get(scenarioName), intCache.get(userId), stringCache.get(actionName), dateTimeCache.get(executionStartDate), intCache.get(executionDuration), ResultStatus.withName(resultStatus), stringCache.get(resultMessage))
					if (isResultInTimeWindow(result))
						buffer += result
				}

				// Else, if the resulting data is not well formated print an error message
				case _ => logger.warn("simulation.log had bad end of file, statistics will be generated but may not be accurate")
			}
		}

		buffer.sortBy(_.executionStartDate.getMillis)
	}

	val simulationRunOn = parseFileNameDateFormat(data.head.runOn)

	val requestNames: Seq[String] = data.map(_.requestName).distinct.filterNot(value => value == END_OF_SCENARIO || value == START_OF_SCENARIO)

	val scenarioNames: Seq[String] = data.map(_.scenarioName).distinct

	val dataIndexedBySendDateWithoutMillis: SortedMap[DateTime, Seq[ResultLine]] = SortedMap(data.groupBy(_.executionStartDate.withMillisOfSecond(0)).toSeq: _*)

	val dataIndexedByReceiveDateWithoutMillis: SortedMap[DateTime, Seq[ResultLine]] = SortedMap(data.groupBy(result => result.executionStartDate.plus(result.executionDurationInMillis).withMillisOfSecond(0)).toSeq: _*)

	def requestData(requestName: String) = data.filter(_.requestName == requestName)

	def scenarioData(scenarioName: String) = data.filter(_.scenarioName == scenarioName)

	def requestDataIndexedBySendDate(requestName: String): SortedMap[DateTime, Seq[ResultLine]] = SortedMap(requestData(requestName).groupBy(_.executionStartDate).toSeq: _*)

	def requestDataIndexedBySendDateWithoutMillis(requestName: String): SortedMap[DateTime, Seq[ResultLine]] = SortedMap(requestData(requestName).groupBy(_.executionStartDate.withMillisOfSecond(0)).toSeq: _*)

	def scenarioDataIndexedBySendDateWithoutMillis(scenarioName: String): SortedMap[DateTime, Seq[ResultLine]] = SortedMap(scenarioData(scenarioName).groupBy(_.executionStartDate.withMillisOfSecond(0)).toSeq: _*)
}
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
import scala.collection.mutable.{ HashMap, Buffer, ArrayBuffer }
import scala.io.Source

import org.joda.time.DateTime

import com.excilys.ebi.gatling.charts.util.OrderingHelper.DateTimeOrdering
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_ENCODING
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogFile
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ResultStatus
import com.excilys.ebi.gatling.core.util.DateHelper.{ parseResultDate, parseFileNameDateFormat }
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR

class DataLoader(runOn: String) extends Logging {

	private val data: Buffer[ResultLine] = {

		val buffer = new ArrayBuffer[ResultLine]

		// use caches in order to reuse String instances instead of holding multiple references of equal Strings
		val stringCache = new HashMap[String, String]
		val intCache = new HashMap[String, Int]
		val dateTimeCache = new HashMap[String, DateTime]

		def cachedString(string: String) = {
			stringCache.get(string).getOrElse {
				val newString = new String(string)
				stringCache.put(newString, newString)
				newString
			}
		}

		def cachedInt(string: String) = {
			intCache.get(string).getOrElse {
				val newString = new String(string)
				val int = newString.toInt
				intCache.put(newString, int)
				int
			}
		}

		def cachedDateTime(string: String) = {
			dateTimeCache.get(string).getOrElse {
				val newString = new String(string)
				val dateTime = parseResultDate(newString)
				dateTimeCache.put(newString, dateTime)
				dateTime
			}
		}

		for (line <- Source.fromFile(simulationLogFile(runOn).jfile, CONFIG_ENCODING).getLines) {
			line.split(TABULATION_SEPARATOR) match {
				// If we have a well formated result
				case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage) =>

					buffer += ResultLine(cachedString(runOn), cachedString(scenarioName), cachedInt(userId), cachedString(actionName), cachedDateTime(executionStartDate), cachedInt(executionDuration), ResultStatus.withName(resultStatus), cachedString(resultMessage))
				// Else, if the resulting data is not well formated print an error message
				case _ => logger.warn("simulation.log had bad end of file, statistics will be generated but may not be accurate")
			}
		}

		buffer.sortBy(_.executionStartDate.getMillis)
	}
	
	val simulationRunOn = parseFileNameDateFormat(data.head.runOn)

	val requestNames: Buffer[String] = data.map(_.requestName).distinct.filterNot(value => value == END_OF_SCENARIO || value == START_OF_SCENARIO)

	val scenarioNames: Buffer[String] = data.map(_.scenarioName).distinct

	val dataIndexedByDateWithoutMillis: SortedMap[DateTime, Buffer[ResultLine]] = SortedMap(data.groupBy(_.executionStartDate.withMillisOfSecond(0)).toSeq: _*)

	def requestData(requestName: String) = data.filter(_.requestName == requestName)

	def scenarioData(scenarioName: String) = data.filter(_.scenarioName == scenarioName)

	def requestDataIndexedByDate(requestName: String): SortedMap[DateTime, Buffer[ResultLine]] = SortedMap(requestData(requestName).groupBy(_.executionStartDate).toSeq: _*)

	def requestDataIndexedByDateWithoutMillis(requestName: String): SortedMap[DateTime, Buffer[ResultLine]] = SortedMap(requestData(requestName).groupBy(_.executionStartDate.withMillisOfSecond(0)).toSeq: _*)

	def scenarioDataIndexedByDateWithoutMillis(scenarioName: String): SortedMap[DateTime, Buffer[ResultLine]] = SortedMap(scenarioData(scenarioName).groupBy(_.executionStartDate.withMillisOfSecond(0)).toSeq: _*)
}
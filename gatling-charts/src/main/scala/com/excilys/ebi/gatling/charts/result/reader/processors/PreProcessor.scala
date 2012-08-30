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
package com.excilys.ebi.gatling.charts.result.reader.processors

import scala.collection.mutable

import com.excilys.ebi.gatling.charts.result.reader.FileDataReader.TABULATION_PATTERN
import com.excilys.ebi.gatling.charts.result.reader.Predef.{ LOG_STEP, SEC_MILLISEC_RATIO, symbolToString }
import com.excilys.ebi.gatling.charts.result.reader.stats.StatsHelper
import com.excilys.ebi.gatling.charts.result.reader.util.FieldsNames._
import com.excilys.ebi.gatling.core.result.message.RecordType.{ ACTION, RUN }
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.util.DateHelper.parseTimestampString

import grizzled.slf4j.Logging

object PreProcessor extends Logging {
	val ACTION_HEADER: List[String] = List(ACTION_TYPE, SCENARIO, ID, REQUEST, EXECUTION_START, EXECUTION_END, REQUEST_END, RESPONSE_START, STATUS)
	val RUN_HEADER: List[String] = List(ACTION_TYPE, DATE, ID, DESCRIPTION)

	def run(inputIterator: Iterator[String], maxPlotPerSerie: Int) = {
		val (actions, runs) = inputIterator.map(TABULATION_PATTERN.split(_)).filter(array => array.head == ACTION || array.head == RUN).partition(_.head == ACTION)

		val (size, min, max) = actions
			.filter(_.length >= ACTION_HEADER.length)
			.foldLeft((0L, Long.MaxValue, Long.MinValue)) { (accumulator, currentAction) =>
				val map = ACTION_HEADER.zip(currentAction).toMap
				val (size, min, max) = accumulator

				if (size % LOG_STEP == 0)
					info("Read " + size + " lines")

				(size + 1, math.min(min, map(EXECUTION_START).toLong), math.max(max, map(EXECUTION_END).toLong))
			}

		val buffer = mutable.ListBuffer[RunRecord]()
		runs
			.filter(_.length >= RUN_HEADER.size)
			.map(RUN_HEADER.zip(_).toMap)
			.foreach(values => buffer += RunRecord(parseTimestampString(values(DATE)), values(ID), values(DESCRIPTION)))

		info("Read " + size + " lines (finished)")

		(max, min, StatsHelper.step(math.floor(min / SEC_MILLISEC_RATIO).toLong, math.ceil(max / SEC_MILLISEC_RATIO).toLong, maxPlotPerSerie) * SEC_MILLISEC_RATIO, size, buffer)
	}
}

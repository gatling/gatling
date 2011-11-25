/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.charts.loader
import scala.collection.immutable.SortedMap
import scala.io.Source
import scala.tools.nsc.io.Path.string2path

import org.joda.time.DateTime

import com.excilys.ebi.gatling.charts.util.OrderingHelper.{ResultOrdering, DateTimeOrdering}
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_ENCODING
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ResultStatus
import com.excilys.ebi.gatling.core.result.writer.FileDataWriter.{GROUPS_SUFFIX, GROUPS_SEPARATOR, GROUPS_PREFIX}
import com.excilys.ebi.gatling.core.util.DateHelper.parseResultDate
import com.excilys.ebi.gatling.core.config.GatlingFiles.{GATLING_SIMULATION_LOG_FILE, GATLING_RESULTS_FOLDER}

class DataLoader(runOn: String) extends Logging {

	val data: List[ResultLine] = {

		var tmpData: List[ResultLine] = Nil

		for (line <- Source.fromFile((GATLING_RESULTS_FOLDER / runOn / GATLING_SIMULATION_LOG_FILE).jfile, CONFIG_ENCODING).getLines) {
			line.split("\t") match {
				// If we have a well formated result
				case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage, groups) =>
					val groupsList = groups.stripPrefix(GROUPS_PREFIX).stripSuffix(GROUPS_SUFFIX).split(GROUPS_SEPARATOR).toList
					tmpData = ResultLine(runOn, scenarioName, userId.toInt, actionName, parseResultDate(executionStartDate), executionDuration.toInt, ResultStatus.withName(resultStatus), resultMessage, groupsList) :: tmpData
				// Else, if the resulting data is not well formated print an error message
				case _ => logger.warn("simulation.log had bad end of file, statistics will be generated but may not be accurate")
			}
		}

		tmpData.sortBy(result => result.executionStartDate.getMillis)
	}

	val dataIndexedByDateInSeconds: SortedMap[DateTime, List[ResultLine]] = SortedMap(data.groupBy(_.executionStartDate.withMillisOfSecond(0)).toSeq: _*)

	val dataIndexedByRequestName: Map[String, List[ResultLine]] = data.groupBy(_.requestName).map { entry => entry._1 -> entry._2.sorted }

	val dataIndexedByRequestNameAndDateInMilliseconds: Map[String, SortedMap[DateTime, List[ResultLine]]] = dataIndexedByRequestName.map { entry => entry._1 -> SortedMap(entry._2.groupBy(_.executionStartDate).map(entry => entry._1 -> entry._2.sorted).toSeq: _*) }

	val dataIndexedByRequestNameAndDateInSeconds: Map[String, SortedMap[DateTime, List[ResultLine]]] = dataIndexedByRequestName.map { entry => entry._1 -> SortedMap(entry._2.groupBy(_.executionStartDate.withMillisOfSecond(0)).toSeq: _*) }

	val dataIndexedByScenarioName: Map[String, List[ResultLine]] = data.groupBy(_.scenarioName)

	val dataIndexedByScenarioNameAndDateInSeconds: Map[String, SortedMap[DateTime, List[ResultLine]]] = dataIndexedByScenarioName.map { entry => entry._1 -> SortedMap(entry._2.groupBy(_.executionStartDate.withMillisOfSecond(0)).toSeq: _*) }

	val requestNames: List[String] = data.map { result => result.requestName }.distinct.filterNot(value => value == END_OF_SCENARIO || value == START_OF_SCENARIO)

	val groupNames: List[String] = data.map { result => result.groups }.flatten
}
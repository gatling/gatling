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
package com.excilys.ebi.gatling.charts.computer

import scala.collection.immutable.SortedMap
import scala.math.pow
import scala.math.sqrt
import org.joda.time.DateTime
import com.excilys.ebi.gatling.charts.loader.ResultLine
import com.excilys.ebi.gatling.charts.util.OrderingHelper.DateTimeOrdering
import com.excilys.ebi.gatling.charts.util.OrderingHelper.ResultOrdering
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ResultStatus.KO
import com.excilys.ebi.gatling.core.result.message.ResultStatus.OK
import com.excilys.ebi.gatling.charts.report.ActiveSessionsReportGenerator

object Computer extends Logging {

	def averageResponseTime(data: List[ResultLine]): Double = data.map(result => result.executionDurationInMillis).sum / data.length.toDouble

	def responseTimeStandardDeviation(data: List[ResultLine]): Double = {
		val avg = averageResponseTime(data)
		sqrt(data.map(result => (pow(result.executionDurationInMillis - avg, 2))).sum / data.length)
	}

	def minResponseTime(data: List[ResultLine]): Int = data.minBy(result => result.executionDurationInMillis).executionDurationInMillis

	def maxResponseTime(data: List[ResultLine]): Int = data.maxBy(result => result.executionDurationInMillis).executionDurationInMillis

	def responseTimeByMillisecondAsList(data: Map[DateTime, List[ResultLine]]): List[(DateTime, Int)] = SortedMap(data.map { entry => entry._1 -> averageResponseTime(entry._2).toInt }.toSeq: _*).toList

	def numberOfRequestsPerSecond(data: Map[DateTime, List[ResultLine]]): Map[DateTime, Int] = SortedMap(data.map(entry => entry._1 -> entry._2.length).toSeq: _*)

	def numberOfRequestsPerSecondAsList(data: Map[DateTime, List[ResultLine]]): List[(DateTime, Int)] = numberOfRequestsPerSecond(data).toList

	def numberOfSuccessfulRequestsPerSecond(data: Map[DateTime, List[ResultLine]]): List[(DateTime, Int)] = {
		numberOfRequestsPerSecondAsList(data.map(entry => entry._1 -> entry._2.filter(result => result.resultStatus.equals(OK))))
	}

	def numberOfFailedRequestsPerSecond(data: Map[DateTime, List[ResultLine]]): List[(DateTime, Int)] = {
		numberOfRequestsPerSecondAsList(data.map(entry => entry._1 -> entry._2.filter(result => result.resultStatus.equals(KO))))
	}

	def numberOfRequestInResponseTimeRange(data: List[ResultLine], lowerBound: Int, higherBound: Int): List[(String, Int)] = {

		val groupNames = List("0 < t < " + lowerBound + "ms", lowerBound + "ms < t < " + higherBound + "ms", higherBound + "ms < t")
		val (firstGroup, mediumGroup, lastGroup) = (groupNames(0), groupNames(1), groupNames(2))

		var grouped = data.groupBy {
			case result if (result.executionDurationInMillis < lowerBound) => firstGroup
			case result if (result.executionDurationInMillis > higherBound) => lastGroup
			case _ => mediumGroup
		}

		// Adds empty sections
		groupNames.map { name =>
			grouped += (name -> grouped.getOrElse(name, Nil))
		}

		grouped.map(entry => entry._1 -> entry._2.length).toList.sorted
	}

	def respTimeAgainstNbOfReqPerSecond(requestsPerSecond: Map[DateTime, Int], requestData: Map[DateTime, List[ResultLine]]): List[(Int, Int)] = {
		requestData.map { entry =>
			val (dateTime, list) = entry
			requestData.get(dateTime).map { list =>
				requestsPerSecond.get(dateTime).get -> averageResponseTime(list).toInt
			}
		}.filter(value => value.isDefined).map(value => value.get).toList
	}

	def numberOfActiveSessionsPerSecondForAScenario(data: Map[DateTime, List[ResultLine]]): List[(DateTime, Int)] = {
		val endsOnly = data.map(entry => entry._1 -> entry._2.filter(result => result.requestName == END_OF_SCENARIO))
		val startsOnly = data.map(entry => entry._1 -> entry._2.filter(result => result.requestName == START_OF_SCENARIO))

		var ct = 0
		SortedMap(data.map { entry =>
			val (dateTime, list) = entry
			list.foreach { result =>
				if (endsOnly.getOrElse(dateTime, Nil).contains(result)) ct -= 1
				if (startsOnly.getOrElse(dateTime, Nil).contains(result)) ct += 1
			}
			(dateTime, ct)
		}.toSeq: _*).toList
	}

	def numberOfActiveSessionsPerSecondByScenario(dataIndexedByScenario: Map[String, Map[DateTime, List[ResultLine]]], dataIndexedByDateInSeconds: Map[DateTime, List[ResultLine]]): Map[String, List[(DateTime, Int)]] = {
		val allScenarioData = dataIndexedByScenario + (ActiveSessionsReportGenerator.ALL_SESSIONS -> dataIndexedByDateInSeconds)
		// Filling the map with each scenario values
		allScenarioData.map { entry =>
			val (scenarioName, scenarioData) = entry
			(scenarioName -> numberOfActiveSessionsPerSecondForAScenario(scenarioData))
		}
	}

}
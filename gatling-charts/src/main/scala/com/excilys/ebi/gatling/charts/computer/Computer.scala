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
import scala.collection.mutable.MutableList

object Computer extends Logging {

	def averageResponseTime(data: MutableList[ResultLine]): Double = data.map(_.executionDurationInMillis).sum / data.length.toDouble

	def responseTimeStandardDeviation(data: MutableList[ResultLine]): Double = {
		val avg = averageResponseTime(data)
		sqrt(data.map(result => pow(result.executionDurationInMillis - avg, 2)).sum / data.length)
	}

	def minResponseTime(data: MutableList[ResultLine]): Int = data.minBy(_.executionDurationInMillis).executionDurationInMillis

	def maxResponseTime(data: MutableList[ResultLine]): Int = data.maxBy(_.executionDurationInMillis).executionDurationInMillis

	def numberOfSuccesses(data: MutableList[ResultLine]): Int = data.filter(_.resultStatus == OK).size

	def responseTimeByMillisecondAsList(data: Map[DateTime, MutableList[ResultLine]]): List[(DateTime, Int)] = SortedMap(data.map { entry => entry._1 -> averageResponseTime(entry._2).toInt }.filterNot(_._2 == 0).toSeq: _*).toList

	def numberOfRequestsPerSecond(data: Map[DateTime, MutableList[ResultLine]]): Map[DateTime, Int] = SortedMap(data.map(entry => entry._1 -> entry._2.length).toSeq: _*)

	def numberOfRequestsPerSecondAsList(data: Map[DateTime, MutableList[ResultLine]]): List[(DateTime, Int)] = numberOfRequestsPerSecond(data).toList

	def numberOfSuccessfulRequestsPerSecond(data: Map[DateTime, MutableList[ResultLine]]): List[(DateTime, Int)] = {
		numberOfRequestsPerSecondAsList(data.map(entry => entry._1 -> entry._2.filter(_.resultStatus == OK)))
	}

	def numberOfFailedRequestsPerSecond(data: Map[DateTime, MutableList[ResultLine]]): List[(DateTime, Int)] = {
		numberOfRequestsPerSecondAsList(data.map(entry => entry._1 -> entry._2.filter(_.resultStatus == KO)))
	}

	def numberOfRequestInResponseTimeRange(data: MutableList[ResultLine], lowerBound: Int, higherBound: Int): List[(String, Int)] = {

		val groupNames = List((1, "t < " + lowerBound + "ms"), (2, lowerBound + "ms < t < " + higherBound + "ms"), (3, higherBound + "ms < t"))
		val (firstGroup, mediumGroup, lastGroup) = (groupNames(0), groupNames(1), groupNames(2))

		var grouped = data.groupBy {
			case result if (result.executionDurationInMillis < lowerBound) => firstGroup
			case result if (result.executionDurationInMillis > higherBound) => lastGroup
			case _ => mediumGroup
		}

		// Adds empty sections
		groupNames.map { name => grouped += (name -> grouped.getOrElse(name, MutableList.empty)) }

		// Computes the number of requests per group
		// Then sorts the list by the order of the groupName
		// Then creates the list to be returned
		grouped.map(entry => (entry._1, entry._2.length)).toList.sortBy(_._1._1).map { entry => (entry._1._2, entry._2) }
	}

	def respTimeAgainstNbOfReqPerSecond(requestsPerSecond: Map[DateTime, Int], requestData: Map[DateTime, MutableList[ResultLine]]): List[(Int, Int)] = {
		requestData.map { entry =>
			val (dateTime, list) = entry
			requestData.get(dateTime).map {
				requestsPerSecond.get(dateTime).get -> averageResponseTime(_).toInt
			}
		}.filter(_.isDefined).map(_.get).toList
	}

	def numberOfActiveSessionsPerSecondForAScenario(data: Map[DateTime, MutableList[ResultLine]]): List[(DateTime, Int)] = {
		val endsOnly = data.map(entry => entry._1 -> entry._2.filter(_.requestName == END_OF_SCENARIO))
		val startsOnly = data.map(entry => entry._1 -> entry._2.filter(_.requestName == START_OF_SCENARIO))

		var ct = 0
		SortedMap(data.map { entry =>
			val (dateTime, list) = entry
			list.foreach { result =>
				if (endsOnly.getOrElse(dateTime, MutableList.empty).contains(result)) ct -= 1
				if (startsOnly.getOrElse(dateTime, MutableList.empty).contains(result)) ct += 1
			}
			(dateTime, ct)
		}.toSeq: _*).toList
	}

	def numberOfActiveSessionsPerSecondByScenario(allScenarioData: MutableList[(String, SortedMap[DateTime, MutableList[ResultLine]])]): MutableList[(String, List[(DateTime, Int)])] = {
		// Filling the map with each scenario values
		allScenarioData.map { entry =>
			val (scenarioName, scenarioData) = entry
			(scenarioName -> numberOfActiveSessionsPerSecondForAScenario(scenarioData))
		}
	}

}
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
package com.excilys.ebi.gatling.charts.computer

import scala.collection.immutable.SortedMap
import scala.math.{sqrt, pow}

import org.joda.time.DateTime

import com.excilys.ebi.gatling.charts.util.OrderingHelper.DateTimeOrdering
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ResultStatus.{ResultStatus, OK, KO}
import com.excilys.ebi.gatling.core.result.writer.ResultLine

object Computer extends Logging {

	def averageResponseTime(data: Seq[ResultLine]): Double = {
		if (data.isEmpty)
			-1d
		else
			data.map(_.executionDurationInMillis).sum / data.length.toDouble
	}

	def responseTimeStandardDeviation(data: Seq[ResultLine]): Double = {
		val avg = averageResponseTime(data)
		sqrt(data.map(result => pow(result.executionDurationInMillis - avg, 2)).sum / data.length)
	}

	def minResponseTime(data: Seq[ResultLine]): Long = data.minBy(_.executionDurationInMillis).executionDurationInMillis

	def maxResponseTime(data: Seq[ResultLine]): Long = data.maxBy(_.executionDurationInMillis).executionDurationInMillis

	def numberOfSuccesses(data: Seq[ResultLine]): Int = data.filter(_.resultStatus == OK).size

	def responseTimeByMillisecondAsList(data: Map[DateTime, Seq[ResultLine]], resultStatus: ResultStatus): List[(DateTime, Int)] =
		SortedMap(data.map(entry => entry._1 -> entry._2.filter(_.resultStatus == resultStatus)).map { entry =>
			val (date, list) = entry
			entry._1 -> averageResponseTime(list).toInt
		}.toSeq: _*).toList

	def numberOfRequestsPerSecond(data: Map[DateTime, Seq[ResultLine]]): Map[DateTime, Int] = SortedMap(data.map(entry => entry._1 -> entry._2.length).toSeq: _*)

	def numberOfRequestsPerSecondAsList(data: Map[DateTime, Seq[ResultLine]]): List[(DateTime, Int)] = numberOfRequestsPerSecond(data).toList

	def numberOfSuccessfulRequestsPerSecond(data: Map[DateTime, Seq[ResultLine]]): List[(DateTime, Int)] = {
		numberOfRequestsPerSecondAsList(data.map(entry => entry._1 -> entry._2.filter(_.resultStatus == OK)))
	}

	def numberOfFailedRequestsPerSecond(data: Map[DateTime, Seq[ResultLine]]): List[(DateTime, Int)] = {
		numberOfRequestsPerSecondAsList(data.map(entry => entry._1 -> entry._2.filter(_.resultStatus == KO)))
	}

	def numberOfRequestInResponseTimeRange(data: Seq[ResultLine], lowerBound: Int, higherBound: Int): List[(String, Int)] = {

		val groupNames = List((1, "t < " + lowerBound + "ms"), (2, lowerBound + "ms < t < " + higherBound + "ms"), (3, higherBound + "ms < t"), (4, "failed"))
		val (firstGroup, mediumGroup, lastGroup, failedGroup) = (groupNames(0), groupNames(1), groupNames(2), groupNames(3))

		var grouped = data.groupBy {
			case result if (result.resultStatus == KO) => failedGroup
			case result if (result.executionDurationInMillis < lowerBound) => firstGroup
			case result if (result.executionDurationInMillis > higherBound) => lastGroup
			case _ => mediumGroup
		}

		// Adds empty sections
		groupNames.map { name => grouped += (name -> grouped.getOrElse(name, Seq.empty)) }

		// Computes the number of requests per group
		// Then sorts the list by the order of the groupName
		// Then creates the list to be returned
		grouped.map(entry => (entry._1, entry._2.length)).toList.sortBy(_._1._1).map { entry => (entry._1._2, entry._2) }
	}

	def respTimeAgainstNbOfReqPerSecond(requestsPerSecond: Map[DateTime, Int], requestData: Map[DateTime, Seq[ResultLine]], resultStatus: ResultStatus): List[(Int, Long)] = {
		requestData.map { entry =>
			val (dateTime, list) = entry
			requestData.get(dateTime).map { list =>
				list.filter(_.resultStatus == resultStatus).map(requestsPerSecond.get(dateTime).get -> _.executionDurationInMillis)
			}
		}.filter(_.isDefined).map(_.get).toList.flatten
	}

	def numberOfActiveSessionsPerSecondForAScenario(data: Map[DateTime, Seq[ResultLine]]): List[(DateTime, Int)] = {
		val endsOnly = data.map(entry => entry._1 -> entry._2.filter(_.requestName == END_OF_SCENARIO))
		val startsOnly = data.map(entry => entry._1 -> entry._2.filter(_.requestName == START_OF_SCENARIO))

		var ct = 0
		SortedMap(data.map { entry =>
			val (dateTime, list) = entry
			list.foreach { result =>
				if (endsOnly.getOrElse(dateTime, List.empty).contains(result)) ct -= 1
				if (startsOnly.getOrElse(dateTime, List.empty).contains(result)) ct += 1
			}
			(dateTime, ct)
		}.toSeq: _*).toList
	}

	def numberOfActiveSessionsPerSecondByScenario(allScenarioData: Seq[(String, SortedMap[DateTime, Seq[ResultLine]])]): Seq[(String, List[(DateTime, Int)])] = {
		// Filling the map with each scenario values
		allScenarioData.map { entry =>
			val (scenarioName, scenarioData) = entry
			(scenarioName -> numberOfActiveSessionsPerSecondForAScenario(scenarioData))
		}
	}

}
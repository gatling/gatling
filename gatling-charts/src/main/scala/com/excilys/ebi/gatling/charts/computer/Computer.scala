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

import scala.annotation.implicitNotFound
import scala.collection.SortedMap
import scala.math.{ sqrt, pow }
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ RequestStatus, OK, KO }
import com.excilys.ebi.gatling.core.result.message.RequestRecord

object Computer {

	val AVERAGE_TIME_NO_PLOT_MAGIC_VALUE = -1

	def averageTime(timeFunction: RequestRecord => Long)(data: Seq[RequestRecord]): Int = if (data.isEmpty) AVERAGE_TIME_NO_PLOT_MAGIC_VALUE else (data.map(timeFunction(_)).sum / data.length.toDouble).toInt

	val averageResponseTime = averageTime(_.responseTime) _

	val averageLatency = averageTime(_.latency) _

	def responseTimeStandardDeviation(data: Seq[RequestRecord]): Double = {
		val avg = averageResponseTime(data)
		if (avg != AVERAGE_TIME_NO_PLOT_MAGIC_VALUE) sqrt(data.map(result => pow(result.responseTime - avg, 2)).sum / data.length) else AVERAGE_TIME_NO_PLOT_MAGIC_VALUE
	}

	def minResponseTime(data: Seq[RequestRecord]): Long = if (data.isEmpty) 0 else data.minBy(_.responseTime).responseTime

	def maxResponseTime(data: Seq[RequestRecord]): Long = if (data.isEmpty) 0 else data.maxBy(_.responseTime).responseTime

	def computationByMillisecondAsList(data: SortedMap[Long, Seq[RequestRecord]], resultStatus: RequestStatus, computation: Seq[RequestRecord] => Int): List[(Long, Int)] =
		data
			.map { case (time, results) => time -> results.filter(_.resultStatus == resultStatus) }
			.map { case (time, results) => time -> computation(results) }
			.toList

	def responseTimeByMillisecondAsList(data: SortedMap[Long, Seq[RequestRecord]], resultStatus: RequestStatus): List[(Long, Int)] = computationByMillisecondAsList(data, resultStatus, averageResponseTime)

	def latencyByMillisecondAsList(data: SortedMap[Long, Seq[RequestRecord]], resultStatus: RequestStatus): List[(Long, Int)] = computationByMillisecondAsList(data, resultStatus, averageLatency)

	def numberOfRequestsPerSecond(data: SortedMap[Long, Seq[RequestRecord]]): SortedMap[Long, Int] = data.map { case (time, results) => time -> results.length }

	def numberOfRequestsPerSecondAsList(data: SortedMap[Long, Seq[RequestRecord]]): List[(Long, Int)] = numberOfRequestsPerSecond(data).toList

	def numberOfRequestsPerSecond(data: SortedMap[Long, Seq[RequestRecord]], resultStatus: RequestStatus): List[(Long, Int)] =
		numberOfRequestsPerSecondAsList(data.map { case (time, results) => time -> results.filter(_.resultStatus == resultStatus) })

	def numberOfRequestInResponseTimeRange(data: Seq[RequestRecord], lowerBound: Int, higherBound: Int): List[(String, Int)] = {

		val groupNames = List((1, "t < " + lowerBound + "ms"), (2, lowerBound + "ms < t < " + higherBound + "ms"), (3, higherBound + "ms < t"), (4, "failed"))
		val (firstGroup, mediumGroup, lastGroup, failedGroup) = (groupNames(0), groupNames(1), groupNames(2), groupNames(3))

		var grouped = data.groupBy {
			case result if (result.resultStatus == KO) => failedGroup
			case result if (result.responseTime < lowerBound) => firstGroup
			case result if (result.responseTime > higherBound) => lastGroup
			case _ => mediumGroup
		}

		// Add empty sections
		groupNames.map { name => grouped += (name -> grouped.getOrElse(name, Seq.empty)) }

		// Computes the number of requests per group
		// Then sorts the list by the order of the groupName
		// Then creates the list to be returned
		grouped
			.map { case (range, results) => (range, results.length) }
			.toList
			.sortBy { case ((rangeId, _), _) => rangeId }
			.map { case ((_, rangeName), count) => (rangeName, count) }
	}

	def respTimeAgainstNbOfReqPerSecond(requestsPerSecond: SortedMap[Long, Int], requestData: SortedMap[Long, Seq[RequestRecord]], resultStatus: RequestStatus): List[(Int, Long)] =
		requestData
			.map {
				case (time, results) => results
					.filter(_.resultStatus == resultStatus)
					.map(requestsPerSecond.get(time).get -> _.responseTime)
			}.toList.flatten

	def numberOfActiveSessionsPerSecondForAScenario(data: SortedMap[Long, Seq[RequestRecord]]): List[(Long, Int)] = {

		def requestByNameGroupByTime(name: String) = data.map { case (time, results) => time -> results.filter(_.requestName == name) }

		val starts = requestByNameGroupByTime(START_OF_SCENARIO)
		val ends = requestByNameGroupByTime(END_OF_SCENARIO)

		var ct = 0

		data.map {
			case (time, results) =>
				results.foreach { result =>
					if (starts.getOrElse(time, List.empty).contains(result)) ct += 1
					else if (ends.getOrElse(time, List.empty).contains(result)) ct -= 1
				}
				(time, ct)
		}.toList
	}

	def numberOfActiveSessionsPerSecondByScenario(allScenarioData: Seq[(String, SortedMap[Long, Seq[RequestRecord]])]): Seq[(String, List[(Long, Int)])] =
		// Filling the map with each scenario values
		allScenarioData.map { case (name, data) => name -> numberOfActiveSessionsPerSecondForAScenario(data) }
}
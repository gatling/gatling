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
package com.excilys.ebi.gatling.charts.util

import scala.annotation.tailrec
import scala.math.{ sqrt, round, pow, min, max }
import scala.util.Sorting

import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ RequestStatus, OK, KO }
import com.excilys.ebi.gatling.core.result.reader.ChartRequestRecord

object StatisticsHelper {

	val NO_PLOT_MAGIC_VALUE = -1

	private def isRecordInScenario(record: ChartRequestRecord, scenarioName: Option[String]) = scenarioName.map(_ == record.scenarioName).getOrElse(true)
	private def isRecordWithRequestName(record: ChartRequestRecord, requestName: Option[String]) = requestName.map(_ == record.requestName).getOrElse(true)
	private def isRecordWithStatus(record: ChartRequestRecord, status: Option[RequestStatus]) = status.map(_ == record.requestStatus).getOrElse(true)
	private def isRealRequest(record: ChartRequestRecord) = record.requestName != START_OF_SCENARIO && record.requestName != END_OF_SCENARIO
	private def meanTime(timeFunction: ChartRequestRecord => Long)(data: Seq[ChartRequestRecord]): Long = if (data.isEmpty) NO_PLOT_MAGIC_VALUE else (data.map(timeFunction(_)).sum / data.length.toDouble).toLong
	private def maxTime(timeFunction: ChartRequestRecord => Long)(data: Seq[ChartRequestRecord]): Long = if (data.isEmpty) NO_PLOT_MAGIC_VALUE else timeFunction(data.maxBy(timeFunction(_)))
	private val meanResponseTime = meanTime(_.responseTime) _
	private val maxResponseTime = meanTime(_.responseTime) _
	private val maxLatency = maxTime(_.latency) _

	/**
	 * Compute the population standard deviation of the provided data.
	 *
	 * @param data is all the ChartRequestRecords from a test run
	 */
	def responseTimeStandardDeviation(data: Seq[ChartRequestRecord]): Long = {
		val avg = meanResponseTime(data)
		if (avg != NO_PLOT_MAGIC_VALUE) sqrt(data.map(result => pow(result.responseTime - avg, 2)).sum / data.length).toLong else NO_PLOT_MAGIC_VALUE
	}

	def respTimeAgainstNbOfReqPerSecond(requestsPerSecond: Map[Long, Int], requestData: Seq[(Long, Seq[ChartRequestRecord])], requestStatus: RequestStatus): List[(Int, Long)] = requestData
		.map {
			case (time, results) => results
				.filter(_.requestStatus == requestStatus)
				.map(requestsPerSecond.get(time).get -> _.responseTime)
		}.toList
		.flatten

	def count(data: Seq[(Long, Int)]) = data.foldLeft(0)((sum, entry) => sum + entry._2)

	def numberOfActiveSessionsPerSecond(requestRecords: Seq[ChartRequestRecord], scenarioName: Option[String]): Seq[(Long, Int)] = {

		val deltas = requestRecords.foldLeft(Map.empty[Long, Int]) { (map: Map[Long, Int], record: ChartRequestRecord) =>

			if (isRecordInScenario(record, scenarioName))
				if (record.requestName == START_OF_SCENARIO) {
					val count = map.getOrElse(record.executionStartDateNoMillis, 0)
					map + (record.executionStartDateNoMillis -> (count + 1))
				} else if (record.requestName == END_OF_SCENARIO) {
					val count = map.getOrElse(record.executionStartDateNoMillis, 0)
					map + (record.executionStartDateNoMillis -> (count - 1))
				} else
					map
			else
				map
		}

		val executionStartDates = requestRecords.foldLeft(Set.empty[Long]) { (set: Set[Long], record: ChartRequestRecord) =>
			if (isRecordInScenario(record, scenarioName))
				set + record.executionStartDateNoMillis
			else
				set
		}.toList.sorted

		@tailrec
		def build(lastCount: Int, times: List[Long], counts: List[(Long, Int)]): List[(Long, Int)] = times match {

			case Nil => counts
			case time :: otherTimes =>
				val newCount = deltas.getOrElse(time, 0) + lastCount
				build(newCount, otherTimes, (time, newCount) :: counts)
		}

		build(0, executionStartDates, Nil).sortBy(_._1)
	}

	def numberOfEventsPerSecond(requestRecords: Seq[ChartRequestRecord], event: ChartRequestRecord => Long, status: Option[RequestStatus], requestName: Option[String]): Map[Long, Int] = {
		requestRecords.foldLeft(Map.empty[Long, Int]) { (map, record) =>

			if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status)) {
				val time = event(record)
				val allEntry = map.getOrElse(time, 0)
				map + (time -> (allEntry + 1))

			} else
				map
		}
	}

	def minResponseTime(records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = {
		val temp = records.foldLeft(Long.MaxValue) { (min, record) =>
			if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status) && record.responseTime < min)
				record.responseTime
			else
				min
		}
		if (temp == Long.MaxValue) NO_PLOT_MAGIC_VALUE else temp
	}

	def maxResponseTime(records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = {
		val temp = records.foldLeft(Long.MinValue) { (max, record) =>
			if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status) && record.responseTime > max)
				record.responseTime
			else
				max
		}
		if (temp == Long.MinValue) NO_PLOT_MAGIC_VALUE else temp
	}

	def countRequests(records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Int =
		records.count { record => isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status) }

	def responseTimeDistribution(records: Seq[ChartRequestRecord], slotsNumber: Int, requestName: Option[String]): (Seq[(Long, Int)], Seq[(Long, Int)]) = {

		val total = countRequests(records, None, requestName)

		if (total != 0) {
			val minTime = minResponseTime(records, None, requestName)
			val maxTime = maxResponseTime(records, None, requestName)

			val width = maxTime - minTime
			val step = max(width / slotsNumber, 1)
			val actualSlotNumber = if (step == 1) width.toInt else slotsNumber

			val (okPercentiles, koPercentiles) = records.foldLeft((Map.empty[Long, Int], Map.empty[Long, Int])) { (maps, record) =>

				if (isRealRequest(record) && isRecordWithRequestName(record, requestName)) {
					val (oks, kos) = maps

					val time = minTime + ((record.responseTime - minTime) / step) * step

					if (record.requestStatus == OK) {
						val okEntry = oks.getOrElse(time, 0)
						val newOks = oks + (time -> (okEntry + 1))
						(newOks, kos)

					} else {
						val koEntry = kos.getOrElse(time, 0)
						val newKos = kos + (time -> (koEntry + 1))
						(oks, newKos)
					}

				} else
					maps
			}

			def distribution(percentiles: Map[Long, Int]): Seq[(Long, Int)] = for (i <- 0 to actualSlotNumber) yield {
				val range = minTime + i * step
				val count = percentiles.get(range).getOrElse(0)
				val percentage = round(count * 100.0 / total).toInt
				(range, percentage)
			}

			(distribution(okPercentiles), distribution(koPercentiles))
		} else
			(Seq.empty, Seq.empty)
	}

	def percentiles(records: Seq[ChartRequestRecord], percentage1: Double, percentage2: Double, status: Option[RequestStatus], requestName: Option[String]): (Long, Long) = {

		def percentile(sortedRecords: Array[Long], percentage: Double): Long = {
			if (sortedRecords.length == 0)
				NO_PLOT_MAGIC_VALUE
			else {
				val limitIndex = min(round(percentage * sortedRecords.length + 0.5).toInt, sortedRecords.length) - 1
				sortedRecords(limitIndex)
			}
		}

		val responseTimes = records.foldLeft(Set.empty[Long]) { (times, record) =>
			if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status)) {
				times + record.responseTime

			} else
				times
		}.toSeq

		val sortedRequests = Sorting.stableSort(responseTimes)

		(percentile(sortedRequests, percentage1), percentile(sortedRequests, percentage2))
	}

	private def meanTime(data: Seq[ChartRequestRecord], timeFunction: ChartRequestRecord => Long, status: Option[RequestStatus], requestName: Option[String]): Long = {

		val count = countRequests(data, status, requestName)

		if (count == 0)
			NO_PLOT_MAGIC_VALUE
		else {
			val sum = data.foldLeft(0L) { (count, record) =>
				if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status)) {
					count + timeFunction(record)
				} else
					count
			}

			sum / count
		}
	}

	def meanResponseTime(data: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = meanTime(data, _.responseTime, status, requestName)

	def meanLatency(data: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = meanTime(data, _.latency, status, requestName)

	/**
	 * Compute the population standard deviation of the provided data.
	 *
	 * @param data is all the ChartRequestRecords from a test run
	 */
	def responseTimeStandardDeviation(data: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = {
		val avg = meanResponseTime(data, status, requestName)
		if (avg != NO_PLOT_MAGIC_VALUE) {
			val sum = data.foldLeft(0.0) { (count, record) =>
				if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status)) {
					count + pow(record.responseTime - avg, 2)
				} else
					count
			}

			val count = countRequests(data, status, requestName)

			sqrt(sum / count).toLong
		} else
			NO_PLOT_MAGIC_VALUE
	}

	def numberOfRequestInResponseTimeRange(data: Seq[ChartRequestRecord], lowerBound: Int, higherBound: Int, requestName: Option[String]): Seq[(String, Int)] = {

		val (firstCount, mediumCount, lastCount, koCount) = data.foldLeft((0, 0, 0, 0)) { (counts, record) =>
			if (isRealRequest(record) && isRecordWithRequestName(record, requestName)) {
				val (firstCount, mediumCount, lastCount, koCount) = counts

				if (record.requestStatus == KO)
					(firstCount, mediumCount, lastCount, koCount + 1)
				else if (record.responseTime < lowerBound)
					(firstCount + 1, mediumCount, lastCount, koCount)
				else if (record.responseTime > higherBound)
					(firstCount, mediumCount, lastCount + 1, koCount)
				else
					(firstCount, mediumCount + 1, lastCount, koCount)

			} else
				counts
		}

		List(("t < " + lowerBound + "ms", firstCount),
			(lowerBound + "ms < t < " + higherBound + "ms", mediumCount),
			(higherBound + "ms < t", lastCount),
			("failed", koCount))
	}

	def requestRecordsGroupByExecutionStartDate(data: Seq[ChartRequestRecord], requestName: String): Seq[(Long, Seq[ChartRequestRecord])] = data
		.filter(_.requestName == requestName)
		.groupBy(_.executionStartDateNoMillis)
		.toSeq

	private def computationOverTime(data: Seq[(Long, Seq[ChartRequestRecord])], requestStatus: RequestStatus, computation: Seq[ChartRequestRecord] => Long): Seq[(Long, Long)] =
		data
			.map { case (time, results) => time -> results.filter(_.requestStatus == requestStatus) }
			.map { case (time, results) => time -> computation(results) }
			.sortBy(_._1)

	def responseTimeOverTime(data: Seq[(Long, Seq[ChartRequestRecord])], requestStatus: RequestStatus): Seq[(Long, Long)] = computationOverTime(data, requestStatus, maxResponseTime)

	def latencyOverTime(data: Seq[(Long, Seq[ChartRequestRecord])], requestStatus: RequestStatus): Seq[(Long, Long)] = computationOverTime(data, requestStatus, maxLatency)
}
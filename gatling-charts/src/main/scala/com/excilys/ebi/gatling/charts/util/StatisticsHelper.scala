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
	private def meanTime(timeFunction: ChartRequestRecord => Long)(records: Seq[ChartRequestRecord]): Long = if (records.isEmpty) NO_PLOT_MAGIC_VALUE else (records.map(timeFunction(_)).sum / records.length.toDouble).toLong
	private def maxTime(timeFunction: ChartRequestRecord => Long)(records: Seq[ChartRequestRecord]): Long = if (records.isEmpty) NO_PLOT_MAGIC_VALUE else timeFunction(records.maxBy(timeFunction(_)))
	private val meanResponseTime = meanTime(_.responseTime) _
	private val maxResponseTime = meanTime(_.responseTime) _
	private val maxLatency = maxTime(_.latency) _

	/**
	 * Compute the population standard deviation of the provided records.
	 *
	 * @param records is all the ChartRequestRecords from a test run
	 */
	def responseTimeStandardDeviation(records: Seq[ChartRequestRecord]): Long = {
		val avg = meanResponseTime(records)
		if (avg != NO_PLOT_MAGIC_VALUE) sqrt(records.map(result => pow(result.responseTime - avg, 2)).sum / records.length).toLong else NO_PLOT_MAGIC_VALUE
	}

	def respTimeAgainstNbOfReqPerSecond(requestsPerSecond: Map[Long, Long], records: Seq[(Long, Seq[ChartRequestRecord])], requestStatus: RequestStatus): Seq[(Long, Long)] = records
		.flatMap {
			case (time, results) => results
				.filter(_.requestStatus == requestStatus)
				.map(requestsPerSecond.get(time).get -> _.responseTime)
		}

	def count(records: Seq[(Long, Long)]) = records.foldLeft(0L)((sum, entry) => sum + entry._2)

	def numberOfActiveSessionsPerSecond(requestRecords: Seq[ChartRequestRecord], scenarioName: Option[String]): Seq[(Long, Long)] = {

		val deltas = requestRecords.foldLeft(Map.empty[Long, Long]) { (map: Map[Long, Long], record: ChartRequestRecord) =>

			if (isRecordInScenario(record, scenarioName))
				if (record.requestName == START_OF_SCENARIO) {
					val count = map.getOrElse(record.executionStartDateNoMillis, 0L)
					map + (record.executionStartDateNoMillis -> (count + 1))
				} else if (record.requestName == END_OF_SCENARIO) {
					val count = map.getOrElse(record.executionStartDateNoMillis, 0L)
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
		def build(lastCount: Long, times: List[Long], counts: List[(Long, Long)]): List[(Long, Long)] = times match {

			case Nil => counts
			case time :: otherTimes =>
				val newCount = deltas.getOrElse(time, 0L) + lastCount
				build(newCount, otherTimes, (time, newCount) :: counts)
		}

		build(0, executionStartDates, Nil).sortBy(_._1)
	}

	def numberOfEventsPerSecond(requestRecords: Seq[ChartRequestRecord], event: ChartRequestRecord => Long, status: Option[RequestStatus], requestName: Option[String]): Map[Long, Long] = {
		requestRecords.foldLeft(Map.empty[Long, Long]) { (map, record) =>

			if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status)) {
				val time = event(record)
				val allEntry = map.getOrElse(time, 0L)
				map + (time -> (allEntry + 1L))

			} else
				map
		}
	}

	def minResponseTime(records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = minTime((record: ChartRequestRecord) => record.responseTime, records, status, requestName)

	def maxResponseTime(records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = maxTime((record: ChartRequestRecord) => record.responseTime, records, status, requestName)

	private def minTime(timeFunction: ChartRequestRecord => Long, records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = {
		val temp = records.foldLeft(Long.MaxValue) { (min, record) =>
			if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status) && timeFunction(record) < min)
				timeFunction(record)
			else
				min
		}
		if (temp == Long.MaxValue) NO_PLOT_MAGIC_VALUE else temp
	}

	private def maxTime(timeFunction: ChartRequestRecord => Long, records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = {
		val temp = records.foldLeft(Long.MinValue) { (max, record) =>
			if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status) && timeFunction(record) > max)
				timeFunction(record)
			else
				max
		}
		if (temp == Long.MinValue) NO_PLOT_MAGIC_VALUE else temp
	}

	def countRequests(records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long =
		records.foldLeft(0L) { (count, record) =>
			if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status))
				count + 1
			else
				count
		} // why the hell does .count return an Int and not a Long?!

	def responseTimeDistribution(records: Seq[ChartRequestRecord], maxSlotsNumber: Int, requestName: Option[String]): (Seq[(Long, Long)], Seq[(Long, Long)]) = {

		val total = countRequests(records, None, requestName)

		if (total != 0) {
			val minTime = minResponseTime(records, None, requestName)
			val maxTime = maxResponseTime(records, None, requestName)

			val width = maxTime - minTime

			val actualSlotNumber = min(width.toInt, 100)
			val step = max(width.toDouble / (maxSlotsNumber - 1), 1.0)

			val (okPercentiles, koPercentiles) = records.foldLeft((Map.empty[Long, Long], Map.empty[Long, Long])) { (maps, record) =>

				if (isRealRequest(record) && isRecordWithRequestName(record, requestName)) {
					val (oks, kos) = maps

					val time: Long = minTime + (((record.responseTime - minTime) / step).toInt * step).toLong

					if (record.requestStatus == OK) {
						val okEntry = oks.getOrElse(time, 0L)
						val newOks = oks + (time -> (okEntry + 1L))
						(newOks, kos)

					} else {
						val koEntry = kos.getOrElse(time, 0L)
						val newKos = kos + (time -> (koEntry + 1L))
						(oks, newKos)
					}

				} else
					maps
			}

			def distribution(percentiles: Map[Long, Long]): Seq[(Long, Long)] = for (i <- 0 to actualSlotNumber + 1) yield {
				val range = (minTime + i * step).toLong
				val count = percentiles.get(range).getOrElse(0L)
				val percentage = round(count * 100.0 / total).toLong
				(range, percentage)
			}

			(distribution(okPercentiles), distribution(koPercentiles))
		} else
			(Seq.empty, Seq.empty)
	}

	def percentiles(records: Seq[ChartRequestRecord], percentage1: Double, percentage2: Double, status: Option[RequestStatus], requestName: Option[String]): (Long, Long) = {

		def percentile(sortedRecords: Array[Long], percentage: Double): Long = {
			if (sortedRecords.isEmpty)
				NO_PLOT_MAGIC_VALUE
			else {
				val limitIndex = min(round(percentage * sortedRecords.length + 0.5).toInt, sortedRecords.length) - 1
				sortedRecords(limitIndex)
			}
		}

		val responseTimes = records.foldLeft(List.empty[Long]) { (times, record) =>
			if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status)) {
				record.responseTime :: times

			} else
				times
		}

		val sortedRequests = Sorting.stableSort(responseTimes)

		(percentile(sortedRequests, percentage1), percentile(sortedRequests, percentage2))
	}

	private def meanTime(records: Seq[ChartRequestRecord], timeFunction: ChartRequestRecord => Long, status: Option[RequestStatus], requestName: Option[String]): Long = {

		val count = countRequests(records, status, requestName)

		if (count == 0)
			NO_PLOT_MAGIC_VALUE
		else {
			val sum = records.foldLeft(0L) { (count, record) =>
				if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status)) {
					count + timeFunction(record)
				} else
					count
			}

			sum / count
		}
	}

	def meanResponseTime(records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = meanTime(records, _.responseTime, status, requestName)

	def meanLatency(records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = meanTime(records, _.latency, status, requestName)

	def meanNumberOfRequestsPerSecond(records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = {

		val count = countRequests(records, status, requestName)

		if (count != 0) {
			val minStartTime = minTime((record: ChartRequestRecord) => record.executionStartDateNoMillis, records, None, None)
			val maxEndTime = maxTime((record: ChartRequestRecord) => record.executionEndDateNoMillis, records, None, None)

			count * 1000L / (maxEndTime - minStartTime)

		} else
			NO_PLOT_MAGIC_VALUE
	}

	/**
	 * Compute the population standard deviation of the provided records.
	 *
	 * @param records is all the ChartRequestRecords from a test run
	 */
	def responseTimeStandardDeviation(records: Seq[ChartRequestRecord], status: Option[RequestStatus], requestName: Option[String]): Long = {
		val avg = meanResponseTime(records, status, requestName)
		if (avg != NO_PLOT_MAGIC_VALUE) {
			val sum = records.foldLeft(0.0) { (count, record) =>
				if (isRealRequest(record) && isRecordWithRequestName(record, requestName) && isRecordWithStatus(record, status)) {
					count + pow(record.responseTime - avg, 2)
				} else
					count
			}

			val count = countRequests(records, status, requestName)

			sqrt(sum / count).toLong
		} else
			NO_PLOT_MAGIC_VALUE
	}

	def numberOfRequestInResponseTimeRange(records: Seq[ChartRequestRecord], lowerBound: Long, higherBound: Long, requestName: Option[String]): Seq[(String, Long)] = {

		val (firstCount, mediumCount, lastCount, koCount) = records.foldLeft((0L, 0L, 0L, 0L)) { (counts, record) =>
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

	def requestRecordsGroupByExecutionStartDate(records: Seq[ChartRequestRecord], requestName: String): Seq[(Long, Seq[ChartRequestRecord])] = records
		.filter(_.requestName == requestName)
		.groupBy(_.executionStartDateNoMillis)
		.toSeq

	private def computationOverTime(records: Seq[(Long, Seq[ChartRequestRecord])], requestStatus: RequestStatus, computation: Seq[ChartRequestRecord] => Long): Seq[(Long, Long)] =
		records
			.map { case (time, results) => time -> results.filter(_.requestStatus == requestStatus) }
			.map { case (time, results) => time -> computation(results) }
			.sortBy(_._1)

	def responseTimeOverTime(records: Seq[(Long, Seq[ChartRequestRecord])], requestStatus: RequestStatus): Seq[(Long, Long)] = computationOverTime(records, requestStatus, maxResponseTime)

	def latencyOverTime(records: Seq[(Long, Seq[ChartRequestRecord])], requestStatus: RequestStatus): Seq[(Long, Long)] = computationOverTime(records, requestStatus, maxLatency)
}
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
package com.excilys.ebi.gatling.charts.result.reader.stats

import com.excilys.ebi.gatling.charts.result.reader.util.ResultBufferType._
import com.excilys.ebi.gatling.core.result.message.RequestStatus

import grizzled.slf4j.Logging

object StatsResultsHelper extends Logging {
	val NO_PLOT_MAGIC_VALUE = -1L

	def getRunRecord(results: StatsResults) = {

		val records = results.getRunRecordBuffer()
		if (records.size != 1) warn("Expecting one and only one RunRecord")
		records.head
	}

	def getRequestNames(results: StatsResults) = results.getRequestBuffer().sortBy(_.executionStart).map(_.request)

	def getScenarioNames(results: StatsResults) = results.getScenarioBuffer().sortBy(_.executionStart).map(_.scenario)

	def getNumberOfActiveSessionsPerSecond(results: StatsResults, scenarioName: Option[String]) = {

		val bufferType = scenarioName.map(_ => BY_SCENARIO).getOrElse(GLOBAL)

		results
			.getSessionBuffer(bufferType)
			.filter(_.scenario == scenarioName)
			.map(sessionRecord => (sessionRecord.executionStart, sessionRecord.size))
	}

	def getRequestsPerSec(results: StatsResults, status: Option[RequestStatus.RequestStatus], requestName: Option[String], buckets: Seq[Long]) = {

		val requestsPerSecBuffer = filterByStatusAndRequest(results
			.getRequestsPerSecBuffer(getResultBufferType(status, requestName)), status, requestName)
			.map(record => (record.executionStartBucket, record.size))

		getEventPerSec(requestsPerSecBuffer, buckets)
	}

	def getTransactionsPerSec(results: StatsResults, status: Option[RequestStatus.RequestStatus], requestName: Option[String], buckets: Seq[Long]) = {

		val transactionPerSecBuffer = filterByStatusAndRequest(results
			.getTransactionPerSecBuffer(getResultBufferType(status, requestName)), status, requestName)
			.map(record => (record.executionEndBucket, record.size))

		getEventPerSec(transactionPerSecBuffer, buckets)
	}

	def getResponseTimeDistribution(results: StatsResults, maxPlots: Int, requestName: Option[String]) = {

		val bufferType = requestName.map(_ => BY_STATUS_AND_REQUEST).getOrElse(BY_STATUS)

		val buffer = results.getResponseTimeDistributionBuffer(bufferType).filter(_.request == requestName)
		val min = buffer.minBy(_.responseTime).responseTime
		val max = buffer.maxBy(_.responseTime).responseTime
		val size = getCountRequests(results, None, requestName)
		val step = StatsHelper.step(min, max, maxPlots)
		val demiStep = step / 2
		val buckets = StatsHelper.bucketsList(min, max, step)
		val (ok, ko) = buffer.partition(_.status == Some(RequestStatus.OK))

		def process(buffer: Seq[ResponseTimeDistributionRecord]) = {
			val distribution = buffer
				.map(record => (StatsHelper.bucket(record.responseTime, min, max, step, demiStep), record))
				.groupBy(_._1)
				.map {
					case (responseTimeBucket, recordList) =>

						val sizeBucket = recordList.foldLeft(0L) {
							(partialSize, record) => partialSize + record._2.size
						}

						(responseTimeBucket, math.round(sizeBucket * 100.0 / size))
				}
				.toSeq
				.sortBy(_._1)

			val (_, output) = buckets.foldLeft((distribution, Seq[(Long, Long)]())) {
				case (accum, current) =>
					val (distribution, output) = accum
					if (!distribution.isEmpty && distribution.head._1 == current) (distribution.tail, output :+ distribution.head)
					else (distribution, output :+ (current, 0L))
			}
			output
		}

		(process(ok), process(ko))
	}

	def getPercentiles(results: StatsResults, percentage1: Double, percentage2: Double, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = {

		val bufferType = getResultBufferType(status, requestName)
		val distributionBuffer = filterByStatusAndRequest(results.getResponseTimeDistributionBuffer(bufferType), status, requestName)
		val statsBuffer = filterByStatusAndRequest(results.getGeneralStatsBuffer(bufferType), status, requestName)
		val percentiles = PercentilesHelper.compute(distributionBuffer, statsBuffer, Seq(percentage1, percentage2))

		(percentiles(0), percentiles(1))
	}

	def getMinResponseTime(results: StatsResults, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(results, _.min, NO_PLOT_MAGIC_VALUE, status, requestName)

	def getMaxResponseTime(results: StatsResults, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(results, _.max, NO_PLOT_MAGIC_VALUE, status, requestName)

	def getCountRequests(results: StatsResults, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(results, _.size, NO_PLOT_MAGIC_VALUE, status, requestName)

	def getMeanResponseTime(results: StatsResults, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(results, r => math.round(r.mean), NO_PLOT_MAGIC_VALUE, status, requestName)

	def getMeanLatency(results: StatsResults, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(results, r => math.round(r.meanLatency), NO_PLOT_MAGIC_VALUE, status, requestName)

	def getMeanNumberOfRequestsPerSecond(results: StatsResults, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(results, r => math.round(r.meanRequestPerSec), NO_PLOT_MAGIC_VALUE, status, requestName)

	def getResponseTimeStandardDeviation(results: StatsResults, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(results, r => math.round(r.stdDev), NO_PLOT_MAGIC_VALUE, status, requestName)

	def getNumberOfRequestInResponseTimeRange(results: StatsResults, lowerBound: Int, higherBound: Int, requestName: Option[String]) = {

		val bufferType = requestName.map(_ => BY_STATUS_AND_REQUEST).getOrElse(BY_STATUS)

		val (ok, ko) = results
			.getResponseTimeDistributionBuffer(bufferType)
			.filter(_.request == requestName)
			.partition(_.status == Some(RequestStatus.OK))

		val (low, middleAndLHigh) = ok.partition(_.responseTime < lowerBound)
		val (middle, high) = middleAndLHigh.partition(_.responseTime <= higherBound)

		def process(label: String, buffer: Seq[ResponseTimeDistributionRecord]) = {
			(label, buffer.foldLeft(0L)((size, record) => size + record.size))
		}

		List(process("t < " + lowerBound + " ms", low),
			process(lowerBound + " ms < t < " + higherBound + " ms", middle),
			process("t > " + higherBound + " ms", high),
			process("failed", ko))
	}

	def getResponseTimeGroupByExecutionStartDate(results: StatsResults, status: RequestStatus.RequestStatus, requestName: String) =
		filterByStatusAndRequest(results
			.getResponseTimePerSecBuffer(BY_STATUS_AND_REQUEST), Some(status), Some(requestName))
			.map(record => (record.executionStartBucket, (record.responseTimeMin, record.responseTimeMax)))

	def getLatencyGroupByExecutionStartDate(results: StatsResults, status: RequestStatus.RequestStatus, requestName: String) =
		filterByStatusAndRequest(results
			.getLatencyPerSecBuffer(BY_STATUS_AND_REQUEST), Some(status), Some(requestName))
			.map(record => (record.executionStartBucket, (record.latencyMin, record.latencyMax)))

	def getRequestAgainstResponseTime(results: StatsResults, status: RequestStatus.RequestStatus, requestName: String) =
		filterByStatusAndRequest(results
			.getRequestAgainstResponseTimeBuffer(BY_STATUS_AND_REQUEST), Some(status), Some(requestName))
			.map(record => (record.size, record.responseTime))

	private def getEventPerSec(eventsPerSec: Seq[(Long, Long)], buckets: Seq[Long]) = {
		val (result, _) = buckets.foldLeft((Seq[(Long, Long)](), eventsPerSec)) {
			(accum, bucket) =>
				val (result, buffer) = accum
				if (buffer.size >= 1 && bucket == buffer.head._1) (result :+ buffer.head, buffer.tail)
				else (result :+ (bucket, 0L), buffer)
		}
		result
	}

	private def getGeneralStat[A](results: StatsResults, statValue: (GeneralStatsRecord) => A, defaultValue: A, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = {
		filterByStatusAndRequest(results
			.getGeneralStatsBuffer(getResultBufferType(status, requestName)), status, requestName)
			.headOption
			.map(statValue(_))
			.getOrElse(defaultValue)
	}

	private def filterByStatusAndRequest[A <: RecordWithStatusAndRequest](buffer: Seq[A], status: Option[RequestStatus.RequestStatus], request: Option[String]) =
		buffer.filter(record => record.status == status && record.request == request)
}

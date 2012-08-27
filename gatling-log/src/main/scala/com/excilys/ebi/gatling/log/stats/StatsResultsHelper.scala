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
package com.excilys.ebi.gatling.log.stats

import grizzled.slf4j.Logging
import com.excilys.ebi.gatling.core.result.message.RequestStatus
import com.excilys.ebi.gatling.log.util.ResultBufferType._
import com.excilys.ebi.gatling.log.processors.PercentilesProcessor

object StatsResultsHelper extends Logging {
	val NO_PLOT_MAGIC_VALUE = -1L

	def getRunRecord = {
		val records = StatsResults.getRunRecordBuffer()
		if (records.size != 1) warn("Expecting one and only one RunRecord")
		records.head
	}

	def getRequestNames = StatsResults.getGeneralStatsBuffer(BY_REQUEST).map(_.request.get)

	def getScenarioNames = StatsResults.getScenarioBuffer().sortBy(_.executionStart).reverse.map(_.scenario)

	def getNumberOfActiveSessionsPerSecond(scenarioName: Option[String]) = {
		val bufferType = scenarioName match {
			case Some(_) => BY_SCENARIO
			case None => GLOBAL
		}
		StatsResults.getSessionBuffer(bufferType).filter(_.scenario == scenarioName).map(sessionRecord => (sessionRecord.executionStart, sessionRecord.size))
	}

	def getRequestsPerSec(status: Option[RequestStatus.RequestStatus], requestName: Option[String], buckets: Seq[Long]) = {
		val requestsPerSecBuffer = filterByStatusAndRequest(StatsResults.getRequestsPerSecBuffer(getResultBufferType(status, requestName)), status, requestName)
			.map(record => (record.executionStartBucket, record.size))
		getEventPerSec(requestsPerSecBuffer, buckets)
	}

	def getTransactionsPerSec(status: Option[RequestStatus.RequestStatus], requestName: Option[String], buckets: Seq[Long]) = {
		val transactionPerSecBuffer = filterByStatusAndRequest(StatsResults.getTransactionPerSecBuffer(getResultBufferType(status, requestName)), status, requestName)
			.map(record => (record.executionEndBucket, record.size))
		getEventPerSec(transactionPerSecBuffer, buckets)
	}

	def getResponseTimeDistribution(maxPlots: Int, requestName: Option[String]) = {
		val bufferType = requestName match {
			case Some(_) => BY_STATUS_AND_REQUEST
			case None => BY_STATUS
		}

		val buffer = StatsResults.getResponseTimeDistributionBuffer(bufferType).filter(_.request == requestName)
		val min = buffer.minBy(_.responseTime).responseTime
		val max = buffer.maxBy(_.responseTime).responseTime
		val size = getCountRequests(None, requestName)
		val step = StatsHelper.step(min, max, maxPlots)
		val demiStep = step / 2
		val buckets = StatsHelper.bucketsList(min, max, step)
		val (ok, ko) = buffer.partition(_.status == Some(RequestStatus.OK))

		def process(buffer: Seq[ResponseTimeDistributionRecord]) = {
			val distribution = buffer
				.map(record => (StatsHelper.bucket(record.responseTime, min, max, step, demiStep), record))
				.groupBy(_._1).map {
				tuple => {
					val (responseTimeBucket, recordList) = tuple
					val sizeBucket = recordList.foldLeft(0L) {
						(partialSize, record) => partialSize + record._2.size
					}
					(responseTimeBucket, math.round(sizeBucket * 100.0 / size))
				}
			}.toSeq.sortBy(_._1)

			val (_, output) = buckets.foldLeft((distribution, Seq[(Long, Long)]())) {
				(accum, current) => {
					val (distribution, output) = accum
					if (!distribution.isEmpty && distribution.head._1 == current) (distribution.tail, output :+ distribution.head)
					else (distribution, output :+(current, 0L))
				}
			}
			output
		}
		(process(ok), process(ko))
	}

	def getPercentiles(percentage1: Double, percentage2: Double, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = {
		val bufferType = getResultBufferType(status, requestName)
		val distributionBuffer = filterByStatusAndRequest(StatsResults.getResponseTimeDistributionBuffer(bufferType), status, requestName)
		val statsBuffer = filterByStatusAndRequest(StatsResults.getGeneralStatsBuffer(bufferType), status, requestName)
		val percentiles = PercentilesProcessor.compute(distributionBuffer, statsBuffer, Seq(percentage1, percentage2))
		(percentiles(0), percentiles(1))
	}

	def getMinResponseTime(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(_.min, NO_PLOT_MAGIC_VALUE, status, requestName)

	def getMaxResponseTime(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(_.max, NO_PLOT_MAGIC_VALUE, status, requestName)

	def getCountRequests(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(_.size, NO_PLOT_MAGIC_VALUE, status, requestName)

	def getMeanResponseTime(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(_.mean.toLong, NO_PLOT_MAGIC_VALUE, status, requestName)

	def getMeanLatency(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(_.meanLatency.toLong, NO_PLOT_MAGIC_VALUE, status, requestName)

	def getMeanNumberOfRequestsPerSecond(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(_.meanRequestPerSec.toLong, NO_PLOT_MAGIC_VALUE, status, requestName)

	def getResponseTimeStandardDeviation(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = getGeneralStat(_.stdDev.toLong, NO_PLOT_MAGIC_VALUE, status, requestName)

	def getNumberOfRequestInResponseTimeRange(lowerBound: Int, higherBound: Int, requestName: Option[String]) = {
		val bufferType = requestName match {
			case Some(_) => BY_STATUS_AND_REQUEST
			case None => BY_STATUS
		}

		val (ok, ko) = StatsResults.getResponseTimeDistributionBuffer(bufferType)
			.filter(_.request == requestName)
			.partition(_.status == Some(RequestStatus.OK))
		val (low, middleAndLHigh) = ok.partition(_.responseTime < lowerBound)
		val (middle, high) = middleAndLHigh.partition(_.responseTime < higherBound)

		def process(label: String, buffer: Seq[ResponseTimeDistributionRecord]) = {
			(label, buffer.foldLeft(0L)((size, record) => size + record.size))
		}

		List(process("t < " + lowerBound + " ms", low),
			process(lowerBound + " ms < t < " + higherBound + " ms", middle),
			process("t > " + higherBound + " ms", high),
			process("failed", ko))
	}

	def getResponseTimeGroupByExecutionStartDate(status: RequestStatus.RequestStatus, requestName: String) =
		filterByStatusAndRequest(StatsResults.getResponseTimePerSecBuffer(BY_STATUS_AND_REQUEST), Some(status), Some(requestName))
			.map(record => (record.executionStartBucket, record.responseTime))

	def getLatencyGroupByExecutionStartDate(status: RequestStatus.RequestStatus, requestName: String) =
		filterByStatusAndRequest(StatsResults.getLatencyPerSecBuffer(BY_STATUS_AND_REQUEST), Some(status), Some(requestName))
			.map(record => (record.executionStartBucket, record.latency))

	def getRequestAgainstResponseTime(status: RequestStatus.RequestStatus, requestName: String) =
		filterByStatusAndRequest(StatsResults.getRequestAgainstResponseTimeBuffer(BY_STATUS_AND_REQUEST), Some(status), Some(requestName))
			.map(record => (record.size, record.responseTime))

	private def getEventPerSec(eventsPerSec: Seq[(Long, Long)], buckets: Seq[Long]) = {
		val (result, _) = buckets.foldLeft((Seq[(Long, Long)](), eventsPerSec)) {
			(accum, bucket) => {
				val (result, buffer) = accum
				if (buffer.size >= 1 && bucket == buffer.head._1) (result :+ buffer.head, buffer.tail)
				else (result :+(bucket, 0L), buffer)
			}
		}
		result
	}

	private def getGeneralStat[A](statValue: (GeneralStatsRecord) => A, defaultValue: A, status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = {
		filterByStatusAndRequest(StatsResults.getGeneralStatsBuffer(getResultBufferType(status, requestName)), status, requestName)
			.headOption match {
			case Some(stats) => statValue(stats)
			case None => defaultValue
		}
	}

	private def filterByStatusAndRequest[A <: RecordWithStatusAndRequest](buffer: Seq[A], status: Option[RequestStatus.RequestStatus], request: Option[String]) =
		buffer.filter(record => record.status == status && record.request == request)
}

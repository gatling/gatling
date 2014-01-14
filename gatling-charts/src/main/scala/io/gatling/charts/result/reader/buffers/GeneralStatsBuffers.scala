/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.charts.result.reader.buffers

import scala.collection.mutable
import io.gatling.charts.result.reader.{ RequestRecord, FileDataReader }
import io.gatling.charts.result.reader.stats.{ PercentilesHelper, StatsHelper }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.Group
import io.gatling.core.result.message.Status
import io.gatling.core.result.reader.GeneralStats
import io.gatling.charts.result.reader.GroupRecord

abstract class GeneralStatsBuffers(durationInSec: Long) {

	val requestGeneralStatsBuffers = mutable.Map.empty[BufferKey, GeneralStatsBuffer]
	val groupDurationGeneralStatsBuffers = mutable.Map.empty[BufferKey, GeneralStatsBuffer]
	val groupCumulatedResponseTimeGeneralStatsBuffers = mutable.Map.empty[BufferKey, GeneralStatsBuffer]
	val requestCounts = mutable.Map.empty[BufferKey, (Int, Int)]

	def getRequestGeneralStatsBuffers(request: Option[String], group: Option[Group], status: Option[Status]): GeneralStatsBuffer =
		requestGeneralStatsBuffers.getOrElseUpdate(BufferKey(request, group, status), new GeneralStatsBuffer(durationInSec))

	def getGroupDurationGeneralStatsBuffers(group: Group, status: Option[Status]): GeneralStatsBuffer =
		groupDurationGeneralStatsBuffers.getOrElseUpdate(BufferKey(None, Some(group), status), new GeneralStatsBuffer(durationInSec))

	def getGroupCumulatedResponseTimeGeneralStatsBuffers(group: Group, status: Option[Status]): GeneralStatsBuffer =
		groupCumulatedResponseTimeGeneralStatsBuffers.getOrElseUpdate(BufferKey(None, Some(group), status), new GeneralStatsBuffer(durationInSec))

	def getGroupRequestCounts(group: Group): (Int, Int) =
		requestCounts.getOrElseUpdate(BufferKey(None, Some(group), None), (0, 0))

	def updateRequestGeneralStatsBuffers(record: RequestRecord) {
		import record._
		getRequestGeneralStatsBuffers(Some(name), group, None).update(responseTime)
		getRequestGeneralStatsBuffers(Some(name), group, Some(status)).update(responseTime)

		getRequestGeneralStatsBuffers(None, None, None).update(responseTime)
		getRequestGeneralStatsBuffers(None, None, Some(status)).update(responseTime)
	}

	def updateGroupGeneralStatsBuffers(record: GroupRecord) {
		import record._
		getGroupCumulatedResponseTimeGeneralStatsBuffers(group, None).update(cumulatedResponseTime)
		getGroupCumulatedResponseTimeGeneralStatsBuffers(group, Some(status)).update(cumulatedResponseTime)
		getGroupDurationGeneralStatsBuffers(group, None).update(duration)
		getGroupDurationGeneralStatsBuffers(group, Some(status)).update(duration)
		val (okCount, koCount) = getGroupRequestCounts(group)
		requestCounts += BufferKey(None, Some(group), None) -> (okCount + oks, koCount + kos)
	}
}

class GeneralStatsBuffer(duration: Long) extends CountBuffer {
	private var min = Int.MaxValue
	private var max = Int.MinValue
	private var count = 0
	private var sum = 0L
	private var squareSum = 0L

	override def update(time: Int) {
		super.update(time)

		if (time < min) min = time
		if (time > max) max = time
		count += 1
		sum += time
		// risk of overflowing Long.MAX_VALUE?
		squareSum += StatsHelper.square(time)
	}

	lazy val stats: GeneralStats =
		if (count == 0) {
			GeneralStats.NO_PLOT

		} else {
			val meanResponseTime = math.round(sum / count.toDouble).toInt
			val meanRequestsPerSec = math.round(count / (duration / FileDataReader.secMillisecRatio)).toInt
			val stdDev = math.round(StatsHelper.stdDev(squareSum / count.toDouble, meanResponseTime)).toInt

			val sortedTimes = map.values.toSeq.sortBy(_.time)

			val percentiles = PercentilesHelper.processPercentiles(sortedTimes, count, Seq(configuration.charting.indicators.percentile1 / 100.0, configuration.charting.indicators.percentile2 / 100.0))

			GeneralStats(min, max, count, meanResponseTime, stdDev, percentiles(0), percentiles(1), meanRequestsPerSec)
		}
}

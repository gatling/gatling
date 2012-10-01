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
package com.excilys.ebi.gatling.charts.result.reader.buffers

import java.util.{ HashMap => JHashMap }

import com.excilys.ebi.gatling.charts.result.reader.ActionRecord
import com.excilys.ebi.gatling.core.result.message.RequestStatus
import scala.collection.JavaConversions.mapAsScalaMap

import com.excilys.ebi.gatling.charts.result.reader.FileDataReader
import com.excilys.ebi.gatling.charts.result.reader.stats.{ PercentilesHelper, StatsHelper }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.reader.GeneralStats

abstract class GeneralStatsBuffers(durationInSec: Long) extends Buffers {

	val generalStatsBuffers = new JHashMap[BufferKey, GeneralStatsBuffer]

	def getGeneralStatsBuffers(requestName: Option[String], status: Option[RequestStatus.RequestStatus]): GeneralStatsBuffer = getBuffer(computeKey(requestName, status), generalStatsBuffers, () => new GeneralStatsBuffer(durationInSec))

	def updateGeneralStatsBuffers(record: ActionRecord) {
		getGeneralStatsBuffers(None, None).update(record.responseTime)
		getGeneralStatsBuffers(None, Some(record.status)).update(record.responseTime)
		getGeneralStatsBuffers(Some(record.request), None).update(record.responseTime)
		getGeneralStatsBuffers(Some(record.request), Some(record.status)).update(record.responseTime)
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

		var stats: GeneralStats = null

		def compute: GeneralStats = {

			if (stats == null) {
				stats = if (count == 0) {
					GeneralStats.NO_PLOT

				} else {
					val meanResponseTime = math.round(sum / count.toDouble).toInt
					val meanRequestsPerSec = math.round(count / (duration / FileDataReader.SEC_MILLISEC_RATIO)).toInt
					val stdDev = math.round(StatsHelper.stdDev(squareSum / count.toDouble, meanResponseTime)).toInt

					val sortedTimes = map.toList.sorted

					val percentiles = PercentilesHelper.processPercentiles(sortedTimes, count, Seq(configuration.charting.indicators.percentile1 / 100.0, configuration.charting.indicators.percentile2 / 100.0))

					GeneralStats(min, max, count, meanResponseTime, stdDev, percentiles(0), percentiles(1), meanRequestsPerSec)
				}
			}

			stats
		}
	}
}


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
package com.excilys.ebi.gatling.log.processors

import annotation.tailrec
import com.excilys.ebi.gatling.log.stats.{ResponseTimeDistributionRecord, GeneralStatsRecord, StatsResultsHelper}

object PercentilesProcessor {

	def compute(distributionBuffer: Seq[ResponseTimeDistributionRecord], generalStatsBuffer: Seq[GeneralStatsRecord], percentiles: Seq[Double]) = {
		val totalSize = generalStatsBuffer.map(_.size).headOption.getOrElse(0L)

		if (totalSize == 0L) percentiles.map(_ => StatsResultsHelper.NO_PLOT_MAGIC_VALUE)
		else processPercentiles(distributionBuffer, totalSize, percentiles)
	}

	private def processPercentiles(buckets: Seq[ResponseTimeDistributionRecord], totalSize: Long, percentilesList: Seq[Double]) = {
		var bucketList = buckets

		var count = 0L

		percentilesList.sorted.map {
			p => {
				val limit = math.round(totalSize * p)
				val (findCount, findBucketList) = findPercentile(bucketList, limit, count)
				bucketList = findBucketList
				count = findCount
				bucketList.head.responseTime
			}
		}
	}

	@tailrec
	private def findPercentile(buckets: Seq[ResponseTimeDistributionRecord], limit: Long, count: Long = 0): (Long, Seq[ResponseTimeDistributionRecord]) = {
		val newCount = count + buckets.head.size

		if (newCount >= limit) (count, buckets)
		else findPercentile(buckets.tail, limit, newCount)
	}
}

case class Bucket(responseTime: Long, size: Long)


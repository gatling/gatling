/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package com.excilys.ebi.gatling.metrics.types

import java.util.{ NavigableMap, TreeMap }

import scala.annotation.tailrec

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RequestRecord
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ KO, OK }

class RequestMetrics {

	val okMetrics = new Metrics(configuration.data.graphite.bucketWidth)
	val koMetrics = new Metrics(configuration.data.graphite.bucketWidth)
	val allMetrics = new Metrics(configuration.data.graphite.bucketWidth)

	def update(requestRecord: RequestRecord) {
		val responseTime = requestRecord.responseTime.max(0L)

		allMetrics.update(responseTime)

		requestRecord.requestStatus match {
			case OK => okMetrics.update(responseTime)
			case KO => koMetrics.update(responseTime)
		}
	}

	def metrics = (okMetrics, koMetrics, allMetrics)

	def reset = {
		okMetrics.reset
		koMetrics.reset
		allMetrics.reset
	}
}

class Metrics(bucketWidth: Int) {

	var count = 0L
	var max = 0L
	var min = Long.MaxValue
	private val buckets: NavigableMap[Long, Long] = new TreeMap[Long, Long]

	def update(value: Long) {
		count += 1
		max = max.max(value)
		min = min.min(value)

		val bucket = value / bucketWidth
		val newCount = if (buckets.containsKey(bucket)) {
			buckets.get(bucket) + 1L
		} else
			1L
		buckets.put(bucket, newCount)
	}

	def reset {
		count = 0L
		max = 0L
		min = Long.MaxValue
		buckets.clear
	}

	def getQuantile(quantile: Int): Long = {
		if (buckets.isEmpty)
			0L
		else {
			val limit = (count * (quantile.toDouble / bucketWidth)).toLong

			@tailrec
			def findQuantile(buckets: NavigableMap[Long, Long], count: Long = 0L): Long = {
				val firstEntry = buckets.firstEntry
				val newCount = count + firstEntry.getValue
				if (newCount >= limit) max.min((firstEntry.getKey * bucketWidth) + bucketWidth)
				else findQuantile(buckets.tailMap(firstEntry.getKey, false), newCount)
			}

			findQuantile(buckets)
		}
	}
}

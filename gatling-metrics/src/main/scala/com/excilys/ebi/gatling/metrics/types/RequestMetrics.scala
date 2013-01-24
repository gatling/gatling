/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import scala.annotation.tailrec

import scala.collection.mutable

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RequestRecord
import com.excilys.ebi.gatling.core.result.message.{ KO, OK }

class RequestMetrics {

	val okMetrics = new Metrics(configuration.graphite.bucketWidth)
	val koMetrics = new Metrics(configuration.graphite.bucketWidth)
	val allMetrics = new Metrics(configuration.graphite.bucketWidth)

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
	private val buckets: mutable.Map[Long,Long] = mutable.HashMap.empty

	def update(value: Long) {
		count += 1
		max = max.max(value)
		min = min.min(value)

		val bucket = value / bucketWidth
		val newCount = buckets.get(bucket).map(_ + 1L).getOrElse(1L)
		buckets += (bucket -> newCount)
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
			def findQuantile(buckets: Seq[(Long, Long)], count: Long = 0L): Long = {
				val firstEntry = buckets.head
				val newCount = count + firstEntry._2
				if (newCount >= limit) max.min((firstEntry._1 * bucketWidth) + bucketWidth)
				else findQuantile(buckets.tail, newCount)
			}

			findQuantile(buckets.toList.sorted)
		}
	}
}

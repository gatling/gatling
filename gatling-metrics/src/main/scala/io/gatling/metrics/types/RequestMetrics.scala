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
package io.gatling.metrics.types

import scala.annotation.tailrec

import scala.collection.mutable

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.result.writer.RequestMessage

class RequestMetrics {

	val okMetrics = new Metrics
	val koMetrics = new Metrics
	val allMetrics = new Metrics

	def update(requestMessage: RequestMessage) {
		val responseTime = requestMessage.responseTime.max(0L)

		allMetrics.update(responseTime)

		requestMessage.status match {
			case OK => okMetrics.update(responseTime)
			case KO => koMetrics.update(responseTime)
		}
	}

	def metrics = (okMetrics, koMetrics, allMetrics)

	def reset() {
		okMetrics.reset
		koMetrics.reset
		allMetrics.reset
	}
}

class Metrics(bucketWidth: Int = configuration.data.graphite.bucketWidth) {

	var count = 0L
	var max = 0L
	var min = Long.MaxValue
	private val buckets = mutable.HashMap.empty[Long, Long].withDefaultValue(0L)

	def update(value: Long) {
		count += 1
		max = max.max(value)
		min = min.min(value)

		val bucket = value / bucketWidth
		val newCount = buckets(bucket) + 1L
		buckets += (bucket -> newCount)
	}

	def reset() {
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
			def getQuantileRec(buckets: List[(Long, Long)], count: Long): Long = buckets match {
				case (bucketTime, bucketCount) :: tail =>
					val newCount = count + bucketCount
					if (newCount >= limit)
						max.min((bucketTime * bucketWidth) + bucketWidth)
					else
						getQuantileRec(tail, newCount)

				case Nil => max
			}

			getQuantileRec(buckets.toList.sorted, 0L)
		}
	}
}

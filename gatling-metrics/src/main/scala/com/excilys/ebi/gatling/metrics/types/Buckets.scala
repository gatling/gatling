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
package com.excilys.ebi.gatling.metrics.types

import java.util.{ NavigableMap, TreeMap }
import annotation.tailrec

class Buckets(bucketWidth: Int) {

	private var count = 0L
	private var max = 0L
	private val buckets: NavigableMap[Long, Long] = new TreeMap[Long, Long]

	def update(responseTime: Long) {
		count += 1
		val bucket = responseTime / bucketWidth
		val newCount = if (buckets.containsKey(bucket)) {
			buckets.get(bucket) + 1L
		} else
			1L
		buckets.put(bucket, newCount)
		max = max.max(responseTime)
	}

	def getQuantile(quantile: Int): Long = {
		if (buckets.isEmpty)
			0L
		else {
			val limit = count * (quantile.toDouble / bucketWidth)
			findQuantile(limit.toLong, buckets)
		}
	}

	@tailrec
	private def findQuantile(limit: Long, buckets: NavigableMap[Long, Long], count: Long = 0L): Long = {
		val firstEntry = buckets.firstEntry
		val newCount = count + firstEntry.getValue
		if (newCount >= limit) max.min((firstEntry.getKey * bucketWidth) + bucketWidth)
		else findQuantile(limit, buckets.tailMap(firstEntry.getKey, false), newCount)
	}

	def reset {
		buckets.clear
	}
}

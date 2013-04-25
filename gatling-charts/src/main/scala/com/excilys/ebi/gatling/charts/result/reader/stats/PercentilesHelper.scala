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
package com.excilys.ebi.gatling.charts.result.reader.stats

import annotation.tailrec

object PercentilesHelper {

	def processPercentiles(buckets: List[(Int, Int)], totalSize: Int, percentiles: Seq[Double]) = {

		@tailrec
		def findPercentile(buckets: List[(Int, Int)], limit: Int, count: Int): (Int, List[(Int, Int)]) = {
			val newCount = count + buckets.head._2

			if (newCount >= limit)
				(count, buckets)
			else
				findPercentile(buckets.tail, limit, newCount)
		}

		var currentBuckets = buckets
		var currentCount = 0

		percentiles.sorted.map {
			p =>
				val limit = math.round(totalSize * p).toInt
				val (foundCount, foundBuckets) = findPercentile(currentBuckets, limit, currentCount)
				currentCount = foundCount
				currentBuckets = foundBuckets
				currentBuckets.head._1
		}
	}
}


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
package com.excilys.ebi.gatling.charts.result.reader

import scala.collection.mutable

import com.excilys.ebi.gatling.core.result.{ Group, IntRangeVsTimePlot, IntVsTimePlot }
import com.excilys.ebi.gatling.core.result.message.RequestStatus

package object buffers {

	type BufferKey = (Option[Group], Option[String], Option[RequestStatus])

	def computeKey(request: Option[String], group: Option[Group], status: Option[RequestStatus]): BufferKey = (group, request, status)

	class CountBuffer {
		val map: mutable.Map[Int, IntVsTimePlot] = mutable.HashMap.empty

		def update(bucket: Int) {
			val current = map.getOrElse(bucket, IntVsTimePlot(bucket, 0))
			map.put(bucket, current.copy(value = current.value + 1))
		}
	}

	class RangeBuffer {
		val map: mutable.Map[Int, IntRangeVsTimePlot] = mutable.HashMap.empty

		def update(bucket: Int, value: Int) {
			val current = map.getOrElse(bucket, IntRangeVsTimePlot(bucket, Int.MaxValue, Int.MinValue))
			map.put(bucket, current.copy(lower = math.min(value, current.lower), higher = math.max(value, current.higher)))
		}
	}
}
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
import io.gatling.charts.result.reader.RequestRecord
import io.gatling.core.result.Group
import io.gatling.core.result.message.{ KO, Status }
import io.gatling.charts.result.reader.GroupRecord

trait ResponseTimeRangeBuffers {

	val responseTimeRangeBuffers = mutable.Map.empty[BufferKey, ResponseTimeRangeBuffer]

	def getResponseTimeRangeBuffers(requestName: Option[String], group: Option[Group]): ResponseTimeRangeBuffer =
		responseTimeRangeBuffers.getOrElseUpdate(BufferKey(requestName, group, None), new ResponseTimeRangeBuffer)

	def updateResponseTimeRangeBuffer(record: RequestRecord) {
		import record._
		getResponseTimeRangeBuffers(Some(name), group).update(responseTime, status)
		getResponseTimeRangeBuffers(None, None).update(responseTime, status)
	}

	def updateGroupResponseTimeRangeBuffer(record: GroupRecord) {
		import record._
		getResponseTimeRangeBuffers(None, Some(group)).update(duration, status)
	}

	class ResponseTimeRangeBuffer {

		import io.gatling.core.config.GatlingConfiguration.configuration

		var low = 0
		var middle = 0
		var high = 0
		var ko = 0

		def update(time: Int, status: Status) {

			if (status == KO) ko += 1
			else if (time < configuration.charting.indicators.lowerBound) low += 1
			else if (time > configuration.charting.indicators.higherBound) high += 1
			else middle += 1
		}
	}

}
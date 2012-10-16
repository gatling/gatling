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

import scala.annotation.tailrec

import com.excilys.ebi.gatling.charts.result.reader.ActionRecord
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.Group
import com.excilys.ebi.gatling.core.result.message.RequestStatus

trait ResponseTimeRangeBuffers extends Buffers {

	val responseTimeRangeBuffers = new JHashMap[BufferKey, ResponseTimeRangeBuffer]

	def getResponseTimeRangeBuffers(requestName: Option[String], group: Option[Group]): ResponseTimeRangeBuffer = getBuffer(computeKey(requestName, group, None), responseTimeRangeBuffers, () => new ResponseTimeRangeBuffer)

	def updateResponseTimeRangeBuffer(record: ActionRecord, group: Option[Group]) {
		recursivelyUpdate(record, group) {
			(record, group) => getResponseTimeRangeBuffers(None, group).update(record)
		}

		getResponseTimeRangeBuffers(Some(record.request), group).update(record)
	}

	class ResponseTimeRangeBuffer {

		import com.excilys.ebi.gatling.core.result.message.RequestStatus
		import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration

		var low = 0
		var middle = 0
		var high = 0
		var ko = 0

		def update(record: ActionRecord) {

			if (record.status == RequestStatus.KO) ko += 1
			else if (record.responseTime < configuration.charting.indicators.lowerBound) low += 1
			else if (record.responseTime > configuration.charting.indicators.higherBound) high += 1
			else middle += 1
		}
	}

}
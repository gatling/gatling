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
package com.excilys.ebi.gatling.charts.result.reader

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RequestStatus
import com.excilys.ebi.gatling.core.result.message.RequestStatus.RequestStatus

object ActionRecord {

	def apply(strings: Array[String], bucketFunction: Long => Long) = {

		def reducePrecision(time: Long) = time / configuration.charting.accuracy * configuration.charting.accuracy

		val scenario = strings(1).intern
		val request = strings(3).intern
		val executionStart = reducePrecision(strings(4).toLong)
		val executionEnd = reducePrecision(strings(5).toLong)
		val requestEnd = reducePrecision(strings(6).toLong)
		val responseStart = reducePrecision(strings(7).toLong)
		val status = RequestStatus.withName(strings(8))
		val executionStartBucket = bucketFunction(executionStart)
		val executionEndBucket = bucketFunction(executionEnd)
		val responseTime = reducePrecision(executionEnd - executionStart)
		val latency = reducePrecision(responseStart - requestEnd)
		new ActionRecord(scenario, request, executionStart, executionEnd, requestEnd, responseStart, status, executionStartBucket, executionEndBucket, responseTime, latency)
	}
}

class ActionRecord(val scenario: String, val request: String, val executionStart: Long, val executionEnd: Long, val requestEnd: Long, val responseStart: Long, val status: RequestStatus, val executionStartBucket: Long, val executionEndBucket: Long, val responseTime: Long, val latency: Long)
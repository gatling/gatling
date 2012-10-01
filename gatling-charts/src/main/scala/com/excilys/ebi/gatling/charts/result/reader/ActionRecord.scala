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

	val accuracyAsDouble = configuration.charting.accuracy.toDouble

	def apply(strings: Array[String], bucketFunction: Int => Int, runStart: Long) = {

		def reduceAccuracy(time: Int): Int = math.round(time / accuracyAsDouble).toInt * configuration.charting.accuracy

		val scenario = strings(1).intern
		val request = strings(3).intern
		val executionStart = reduceAccuracy((strings(4).toLong - runStart).toInt)
		val executionEnd = reduceAccuracy((strings(5).toLong - runStart).toInt)
		val requestEnd = reduceAccuracy((strings(6).toLong - runStart).toInt)
		val responseStart = reduceAccuracy((strings(7).toLong - runStart).toInt)
		val status = RequestStatus.withName(strings(8))
		val executionStartBucket = bucketFunction(executionStart)
		val executionEndBucket = bucketFunction(executionEnd)
		val responseTime = reduceAccuracy(executionEnd - executionStart)
		val latency = reduceAccuracy(responseStart - requestEnd)
		new ActionRecord(scenario, request, executionStart, executionEnd, requestEnd, responseStart, status, executionStartBucket, executionEndBucket, responseTime, latency)
	}
}

class ActionRecord(val scenario: String, val request: String, val executionStart: Int, val executionEnd: Int, val requestEnd: Int, val responseStart: Int, val status: RequestStatus, val executionStartBucket: Int, val executionEndBucket: Int, val responseTime: Int, val latency: Int)
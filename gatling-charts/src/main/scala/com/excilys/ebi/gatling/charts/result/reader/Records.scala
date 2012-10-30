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

import com.excilys.ebi.gatling.core.result.message.RequestStatus
import com.excilys.ebi.gatling.core.result.message.RequestStatus.RequestStatus

object ActionRecord {

	def apply(strings: Array[String], bucketFunction: Int => Int, runStart: Long) = {

		val scenario = strings(1).intern
		val user = strings(2).toInt
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
		new ActionRecord(scenario, user, request, executionStart, executionEnd, requestEnd, responseStart, status, executionStartBucket, executionEndBucket, responseTime, latency)
	}
}

class ActionRecord(val scenario: String, val user: Int, var request: String, val executionStart: Int, val executionEnd: Int, val requestEnd: Int, val responseStart: Int, val status: RequestStatus, val executionStartBucket: Int, val executionEndBucket: Int, val responseTime: Int, val latency: Int)

object ScenarioRecord {
	def apply(strings: Array[String], bucketFunction: Int => Int, runStart: Long) = {

		val scenario = strings(1).intern
		val user = strings(2).toInt
		val subType = strings(3).intern
		val executionDate = reduceAccuracy((strings(4).toLong - runStart).toInt)
		val executionDateBucket = bucketFunction(executionDate)
		new ScenarioRecord(scenario, user, subType, executionDate, executionDateBucket)
	}
}

class ScenarioRecord(val scenario: String, val user: Int, val subType: String, val executionDate: Int, val executionDateBucket: Int)

object GroupRecord {
	def apply(strings: Array[String], bucketFunction: Int => Int, runStart: Long) = {

		val scenario = strings(1).intern
		val user = strings(2).toInt
		val subType = strings(3).intern
		val group = strings(4).intern
		val executionDate = reduceAccuracy((strings(5).toLong - runStart).toInt)
		val executionDateBucket = bucketFunction(executionDate)
		new GroupRecord(scenario, user, subType, executionDate, executionDateBucket, group)
	}
}

class GroupRecord(val scenario: String, val user: Int, val subType: String, val executionDate: Int, val executionDateBucket: Int, val group: String)


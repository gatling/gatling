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

import com.excilys.ebi.gatling.core.result.message.{ KO, OK, RequestStatus }

object ActionRecord {

	def apply(strings: Array[String], bucketFunction: Int => Int, runStart: Long): ActionRecord = {

		val scenario = strings(1).intern
		val user = strings(2).toInt
		val request = strings(3).intern
		val executionStart = reduceAccuracy((strings(4).toLong - runStart).toInt)
		val requestEnd = reduceAccuracy((strings(5).toLong - runStart).toInt)
		val responseStart = reduceAccuracy((strings(6).toLong - runStart).toInt)
		val executionEnd = reduceAccuracy((strings(7).toLong - runStart).toInt)
		val status = strings(8) match {
			case "OK" => OK
			case _ => KO
		}
		val executionStartBucket = bucketFunction(executionStart)
		val executionEndBucket = bucketFunction(executionEnd)
		val responseTime = reduceAccuracy(executionEnd - executionStart)
		val latency = reduceAccuracy(responseStart - requestEnd)
		ActionRecord(scenario, user, request, executionStart, requestEnd, responseStart, executionEnd, status, executionStartBucket, executionEndBucket, responseTime, latency)
	}
}

case class ActionRecord(scenario: String, user: Int, request: String, executionStart: Int, requestEnd: Int, responseStart: Int, executionEnd: Int, status: RequestStatus, executionStartBucket: Int, executionEndBucket: Int, responseTime: Int, latency: Int)

object ScenarioRecord {
	def apply(strings: Array[String], bucketFunction: Int => Int, runStart: Long): ScenarioRecord = {

		val scenario = strings(1).intern
		val user = strings(2).toInt
		val event = strings(3).intern
		val executionDate = reduceAccuracy((strings(4).toLong - runStart).toInt)
		val executionDateBucket = bucketFunction(executionDate)
		ScenarioRecord(scenario, user, event, executionDate, executionDateBucket)
	}
}

case class ScenarioRecord(scenario: String, user: Int, event: String, executionDate: Int, executionDateBucket: Int)

object GroupRecord {
	def apply(strings: Array[String], bucketFunction: Int => Int, runStart: Long): GroupRecord = {

		val scenario = strings(1).intern
		val group = strings(2).intern
		val user = strings(3).toInt
		val event = strings(4).intern
		val executionDate = reduceAccuracy((strings(5).toLong - runStart).toInt)
		val executionDateBucket = bucketFunction(executionDate)
		GroupRecord(scenario, group, user, event, executionDate, executionDateBucket)
	}
}

case class GroupRecord(scenario: String, group: String, user: Int, event: String, executionDate: Int, executionDateBucket: Int)


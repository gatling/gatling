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
package com.excilys.ebi.gatling.charts.result.reader.util

object FieldsNames {
	/* Input fields */
	val ACTION_TYPE = 'actionType
	val SCENARIO = 'scenario
	val ID = 'id
	val REQUEST = 'request
	val STATUS = 'status
	val ERROR = 'error
	val EXECUTION_START = 'executionStart
	val EXECUTION_END = 'executionEnd
	val RESPONSE_START = 'responseStart
	val REQUEST_END = 'requestEnd
	val DATE = 'date
	val DESCRIPTION = 'description
	/* Created fields */
	val EXECUTION_START_BUCKET = 'executionStartBucket
	val EXECUTION_END_BUCKET = 'executionEndBucket
	val RESPONSE_START_BUCKET = 'responseStartBucket
	val REQUEST_END_BUCKET = 'requestEndBucket
	val RESPONSE_TIME_BUCKET = 'responseTimeBucket
	val RESPONSE_TIME = 'responseTime
	val RESPONSE_TIME_MIN = 'responseTimeMin
	val RESPONSE_TIME_MAX = 'responseTimeMax
	val RESPONSE_TIME_LIST = 'responseTimeList
	val LATENCY = 'latency
	val LATENCY_MIN = 'latencyMin
	val LATENCY_MAX = 'latencyMax
	val SQUARE_RESPONSE_TIME = 'squareResponseTime
	val MEAN_REQUEST_PER_SEC = 'meanRequestPerSec
	val MEAN = 'mean
	val SQUARE_MEAN = 'squareMean
	val STD_DEV = 'stdDev
	val MIN = 'min
	val MAX = 'max
	val SIZE = 'size
	val STEP = 'step
	val DELTA = 'delta
	val MEAN_LATENCY = 'meanLatency
}

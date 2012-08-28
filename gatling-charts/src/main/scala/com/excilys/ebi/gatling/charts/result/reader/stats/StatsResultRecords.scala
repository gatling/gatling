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
package com.excilys.ebi.gatling.charts.result.reader.stats

import com.excilys.ebi.gatling.core.result.message.RequestStatus

class RecordWithStatusAndRequest(val status: Option[RequestStatus.RequestStatus], val request: Option[String])

class GeneralStatsRecord(val min: Long, val max: Long, val size: Long, val mean: Double, val meanLatency: Double, val meanRequestPerSec: Double, val stdDev: Double, status: Option[RequestStatus.RequestStatus], request: Option[String]) extends RecordWithStatusAndRequest(status, request)

class ResponseTimeDistributionRecord(val responseTime: Long, val size: Long, status: Option[RequestStatus.RequestStatus], request: Option[String]) extends RecordWithStatusAndRequest(status, request)

class RequestsPerSecRecord(val executionStartBucket: Long, val size: Long, status: Option[RequestStatus.RequestStatus], request: Option[String]) extends RecordWithStatusAndRequest(status, request)

class TransactionsPerSecRecord(val executionEndBucket: Long, val size: Long, status: Option[RequestStatus.RequestStatus], request: Option[String]) extends RecordWithStatusAndRequest(status, request)

class ResponseTimePerSecRecord(val executionStartBucket: Long, val responseTimeMin: Long, val responseTimeMax: Long, status: Option[RequestStatus.RequestStatus], request: Option[String]) extends RecordWithStatusAndRequest(status, request)

class LatencyPerSecRecord(val executionStartBucket: Long, val latencyMin: Long, val latencyMax: Long, status: Option[RequestStatus.RequestStatus], request: Option[String]) extends RecordWithStatusAndRequest(status, request)

class RequestAgainstResponseTimeRecord(val size: Long, val responseTime: Long, status: Option[RequestStatus.RequestStatus], request: Option[String]) extends RecordWithStatusAndRequest(status, request)

class SessionDeltaRecord(val executionStartBucket: Long, val delta: Long, val scenario: Option[String])

class SessionRecord(val executionStart: Long, val size: Long, val scenario: Option[String])

class ScenarioRecord(val scenario: String, val executionStart: Long)

class RequestRecord(val request: String, val executionStart: Long)


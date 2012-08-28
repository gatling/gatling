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
package com.excilys.ebi.gatling.metrics.actors

import com.excilys.ebi.gatling.core.result.message.RequestRecord
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ KO, OK }
import com.excilys.ebi.gatling.metrics.core.Instrumented

import akka.actor.Actor
import grizzled.slf4j.Logging

class RequestMetrics(requestName: String) extends Actor with Logging with Instrumented {

	private val latencyOK = metrics.fastHistogram(requestName + "_latencyOK")
	private val latencyKO = metrics.fastHistogram(requestName + "_latencyKO")
	private val latencyGlobal = metrics.fastHistogram(requestName + "_latencyGlobal")
	private val responseTimeOK = metrics.fastHistogram(requestName + "_responseTimeOK")
	private val responseTimeKO = metrics.fastHistogram(requestName + "_responseTimeKO")
	private val responseTimeGlobal = metrics.fastHistogram(requestName + "_responseTimeGlobal")

	private val latencyOKPerSecond = metrics.cachedFastHistogram(requestName + "_latencyOKPerSecond")
	private val latencyKOPerSecond = metrics.cachedFastHistogram(requestName + "_latencyKOPerSecond")
	private val latencyGlobalPerSecond = metrics.cachedFastHistogram(requestName + "_latencyGlobalPerSecond")
	private val responseTimeOKPerSecond = metrics.cachedFastHistogram(requestName + "_responseTimeOKPerSecond")
	private val responseTimeKOPerSecond = metrics.cachedFastHistogram(requestName + "_responseTimeKOPerSecond")
	private val responseTimeGlobalPerSecond = metrics.cachedFastHistogram(requestName + "_responseTimeGlobalPerSecond")
	private val transactionsOKPerSecond = metrics.cachedFastCounter(requestName + "_transactionsOKPerSecond")
	private val transactionsKOPerSecond = metrics.cachedFastCounter(requestName + "_transactionsKOPerSecond")
	private val transactionsGlobalPerSecond = metrics.cachedFastCounter(requestName + "_transactionsGlobalPerSecond")

	def updateHistograms(requestRecord: RequestRecord) {
		// Update global histograms
		latencyGlobal += requestRecord.latency
		latencyGlobalPerSecond += requestRecord.latency
		responseTimeGlobal += requestRecord.responseTime
		responseTimeGlobalPerSecond += requestRecord.responseTime
		transactionsGlobalPerSecond += 1
		// Update OK or KO histograms, depending on the request status
		requestRecord.requestStatus match {
			case OK => {
				latencyOK += requestRecord.latency
				latencyOKPerSecond += requestRecord.latency
				responseTimeOK += requestRecord.responseTime
				responseTimeOKPerSecond += requestRecord.responseTime
				transactionsOKPerSecond += 1
			}
			case KO => {
				latencyKO += requestRecord.latency
				latencyKOPerSecond += requestRecord.latency
				responseTimeKO += requestRecord.responseTime
				responseTimeKOPerSecond += requestRecord.responseTime
				transactionsKOPerSecond += 1
			}
		}
	}

	def receive = {
		case requestRecord: RequestRecord => updateHistograms(requestRecord)
		case ClearHistograms => clearPerSecondHistograms
	}

	def clearPerSecondHistograms {
		latencyOKPerSecond.clear
		latencyKOPerSecond.clear
		latencyGlobalPerSecond.clear
		responseTimeOKPerSecond.clear
		responseTimeKOPerSecond.clear
		responseTimeGlobalPerSecond.clear
		transactionsGlobalPerSecond.clear
		transactionsOKPerSecond.clear
		transactionsKOPerSecond.clear
	}
}
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
package com.excilys.ebi.gatling.metrics.types

import com.excilys.ebi.gatling.core.result.message.RequestRecord
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ KO, OK }

class RequestMetric {

	private var allCount, okCount, koCount = 0L
	private var allMax, okMax, koMax = 0L
	private val _percentiles = SamplesByStatus(new Sample, new Sample, new Sample)

	def update(requestRecord: RequestRecord) {
		allCount = allCount + 1
		allMax = allMax.max(requestRecord.responseTime)
		requestRecord.requestStatus match {
			case OK =>
				okCount = okCount + 1
				okMax = okMax.max(requestRecord.responseTime)
			case KO =>
				koCount = koCount + 1
				koMax = koMax.max(requestRecord.responseTime)
		}
	}

	def counts = (allCount, okCount, koCount)

	def maxes = {
		val currentMaxes = (allMax, okMax, koMax)
		allMax = 0L
		okMax = 0L
		koMax = 0L
		currentMaxes
	}

	def percentiles = _percentiles
}

case class SamplesByStatus(all: Sample, ko: Sample, ok: Sample)
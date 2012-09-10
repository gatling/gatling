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
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{KO, OK}

class RequestMetric {

	private var _count = MetricsByStatus(0L, 0L, 0L)
	private var _max = MetricsByStatus(-1, -1, -1)
	private val _percentiles = SamplesByStatus(new Sample, new Sample, new Sample)

	def update(requestRecord: RequestRecord) {
		_count = _count.copy(all = _count.all + 1)
		_max = _max.copy(all = _max.all max requestRecord.responseTime)
		_percentiles.all.update(requestRecord.responseTime)
		requestRecord.requestStatus match {
			case OK => {
				_count = _count.copy(ok = _count.ok + 1)
				_max = _max.copy(ok = _max.ok max requestRecord.responseTime)
				_percentiles.ok.update(requestRecord.responseTime)
			}
			case KO => {
				_count = _count.copy(ko = _count.ko + 1)
				_max = _max.copy(ko = _max.ko max requestRecord.responseTime)
				_percentiles.ko.update(requestRecord.responseTime)
			}
		}
	}

	def count = _count

	def max = _max

	def percentiles = _percentiles
}

case class MetricsByStatus(all : Long,ok: Long,ko : Long)
case class SamplesByStatus(all: Sample,ko : Sample, ok : Sample)
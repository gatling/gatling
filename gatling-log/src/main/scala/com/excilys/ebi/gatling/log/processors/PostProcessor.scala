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
package com.excilys.ebi.gatling.log.processors

import grizzled.slf4j.Logging
import com.excilys.ebi.gatling.log.stats.{StatsResults, SessionRecord, SessionDeltaRecord}
import collection.mutable
import com.excilys.ebi.gatling.log.util.ResultBufferType

object PostProcessor extends Logging {

	def run(results: StatsResults, buckets: Seq[Long]) = {
		compute(results.getSessionDeltaBuffer(ResultBufferType.GLOBAL), results.getSessionBuffer(ResultBufferType.GLOBAL), buckets)

		val sessionBufferByScenario = results.getSessionBuffer(ResultBufferType.BY_SCENARIO)
		results.getSessionDeltaBuffer(ResultBufferType.BY_SCENARIO).groupBy(_.scenario).foreach {
			case (scenario, deltas) => compute(deltas, sessionBufferByScenario, buckets, scenario)
		}

		results
	}

	private def compute(sessionDeltaBuffer: Seq[SessionDeltaRecord], sessionBuffer: mutable.Buffer[SessionRecord], buckets: Seq[Long], scenario: Option[String] = None) {
		buckets.foldLeft((0L, sessionDeltaBuffer)) {
			(accumulator, currentBucket) => {
				val (sessions, deltas) = accumulator

				if (deltas.size >= 1 && currentBucket == deltas.head.executionStartBucket) {
					val current = deltas.head
					sessionBuffer += new SessionRecord(current.executionStartBucket, sessions + current.delta, current.scenario)
					(sessions + current.delta, deltas.tail)
				}
				else {
					sessionBuffer += new SessionRecord(currentBucket, sessions, scenario)
					(sessions, deltas)
				}
			}
		}
	}
}

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
package com.excilys.ebi.gatling.charts.result.reader.processors

import scala.collection.mutable

import com.excilys.ebi.gatling.charts.result.reader.stats.{ SessionDeltaRecord, SessionRecord, StatsResults }
import com.excilys.ebi.gatling.charts.result.reader.util.ResultBufferType

import grizzled.slf4j.Logging

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
		buckets.foldLeft((0L, 0L, sessionDeltaBuffer)) { (accumulator, currentBucket) =>
			val (actualActiveSessions, previousBucketNbOfEndSessions, sessionDeltas) = accumulator

			if (sessionDeltas.size >= 1 && currentBucket == sessionDeltas.head.executionStartBucket) {
				val current = sessionDeltas.head
				val newActiveSession = actualActiveSessions + current.nbSessionStart - previousBucketNbOfEndSessions
				sessionBuffer += new SessionRecord(current.executionStartBucket, newActiveSession, current.scenario)
				(newActiveSession, current.nbSessionEnd, sessionDeltas.tail)

			} else {
				val newActiveSession = actualActiveSessions - previousBucketNbOfEndSessions
				sessionBuffer += new SessionRecord(currentBucket, newActiveSession, scenario)
				(newActiveSession, 0L, sessionDeltas)
			}
		}
	}
}

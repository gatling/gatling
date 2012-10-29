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
package com.excilys.ebi.gatling.charts.result.reader.buffers

import com.excilys.ebi.gatling.charts.result.reader.ActionRecord
import com.excilys.ebi.gatling.charts.util.JMap

trait SessionDeltaPerSecBuffers extends Buffers {

	val sessionDeltaPerSecBuffers = new JMap[Option[String], SessionDeltaBuffer]

	def getSessionDeltaPerSecBuffers(scenarioName: Option[String]): SessionDeltaBuffer = sessionDeltaPerSecBuffers.getOrElseUpdate(scenarioName, new SessionDeltaBuffer)

	def addStartSessionBuffers(record: ActionRecord) {
		getSessionDeltaPerSecBuffers(None).addStart(record.executionStartBucket)
		getSessionDeltaPerSecBuffers(Some(record.scenario)).addStart(record.executionStartBucket)
	}

	def addEndSessionBuffers(record: ActionRecord) {
		getSessionDeltaPerSecBuffers(None).addEnd(record.executionStartBucket)
		getSessionDeltaPerSecBuffers(Some(record.scenario)).addEnd(record.executionStartBucket)
	}

	class SessionDeltaBuffer {

		val map = new JMap[Int, (Int, Int)]

		def addStart(bucket: Int) { map.putOrUpdate(bucket, (1, 0), (startEnd: (Int, Int)) => (startEnd._1 + 1, startEnd._2)) }

		def addEnd(bucket: Int) { map.putOrUpdate(bucket, (0, 1), (startEnd: (Int, Int)) => (startEnd._1, startEnd._2 + 1)) }

		def compute(buckets: List[Int]): List[(Int, Int)] = {

			val (_, _, sessions) = buckets.foldLeft(0, 0, List.empty[(Int, Int)]) { (accumulator, bucket) =>
				val (previousSessions, previousEnds, sessions) = accumulator
				val (bucketStarts, bucketEnds) = map.getOrElse(bucket, (0, 0))
				val bucketSessions = previousSessions - previousEnds + bucketStarts
				(bucketSessions, bucketEnds, (bucket, bucketSessions) :: sessions)
			}

			sessions.reverse
		}
	}
}
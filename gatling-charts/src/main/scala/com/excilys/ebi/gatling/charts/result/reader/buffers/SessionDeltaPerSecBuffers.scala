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

import java.util.{ HashMap => JHashMap }

import com.excilys.ebi.gatling.charts.result.reader.ActionRecord

trait SessionDeltaPerSecBuffers extends Buffers {

	val sessionDeltaPerSecBuffers = new JHashMap[BufferKey, SessionDeltaBuffer]

	def getSessionDeltaPerSecBuffers(scenarioName: Option[String]): SessionDeltaBuffer = getBuffer(computeKey(scenarioName, None), sessionDeltaPerSecBuffers, () => new SessionDeltaBuffer)

	def addStartSessionBuffers(record: ActionRecord) {
		getSessionDeltaPerSecBuffers(None).addStart(record.executionStartBucket)
		getSessionDeltaPerSecBuffers(Some(record.scenario)).addStart(record.executionStartBucket)
	}

	def addEndSessionBuffers(record: ActionRecord) {
		getSessionDeltaPerSecBuffers(None).addEnd(record.executionStartBucket)
		getSessionDeltaPerSecBuffers(Some(record.scenario)).addEnd(record.executionStartBucket)
	}

	class SessionDeltaBuffer {

		import scala.collection.JavaConversions.mapAsScalaMap

		implicit val map = new JHashMap[Long, (Long, Long)]

		def addStart(bucket: Long) { initOrUpdateJHashMapEntry(bucket, (1L, 0L), (startEnd: (Long, Long)) => (startEnd._1 + 1, startEnd._2)) }

		def addEnd(bucket: Long) { initOrUpdateJHashMapEntry(bucket, (0L, 1L), (startEnd: (Long, Long)) => (startEnd._1, startEnd._2 + 1)) }

		def compute(buckets: List[Long]): List[(Long, Long)] = {

			val (_, _, sessions) = buckets.foldLeft(0L, 0L, List.empty[(Long, Long)]) { (accumulator, bucket) =>
				val (previousSessions, previousEnds, sessions) = accumulator
				val (bucketStarts, bucketEnds) = map.getOrElse(bucket, (0L, 0L))
				val bucketSessions = previousSessions - previousEnds + bucketStarts
				(bucketSessions, bucketEnds, (bucket, bucketSessions) :: sessions)
			}

			sessions.reverse
		}
	}
}
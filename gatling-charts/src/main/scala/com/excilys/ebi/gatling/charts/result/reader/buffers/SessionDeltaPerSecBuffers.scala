/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import scala.collection.mutable

import com.excilys.ebi.gatling.charts.result.reader.ScenarioRecord
import com.excilys.ebi.gatling.core.result.IntVsTimePlot

trait SessionDeltaPerSecBuffers {

	val sessionDeltaPerSecBuffers: mutable.Map[Option[String], SessionDeltaBuffer] = mutable.HashMap.empty

	def getSessionDeltaPerSecBuffers(scenarioName: Option[String]): SessionDeltaBuffer = sessionDeltaPerSecBuffers.getOrElseUpdate(scenarioName, new SessionDeltaBuffer)

	def addStartSessionBuffers(record: ScenarioRecord) {
		getSessionDeltaPerSecBuffers(None).addStart(record.executionDateBucket)
		getSessionDeltaPerSecBuffers(Some(record.scenario)).addStart(record.executionDateBucket)
	}

	def addEndSessionBuffers(record: ScenarioRecord) {
		getSessionDeltaPerSecBuffers(None).addEnd(record.executionDateBucket)
		getSessionDeltaPerSecBuffers(Some(record.scenario)).addEnd(record.executionDateBucket)
	}

	class SessionDeltaBuffer {

		val startingPoint = (0, 0)
		val map: mutable.Map[Int, (Int, Int)] = mutable.HashMap.empty

		def addStart(bucket: Int) {
			val (start, end) = map.getOrElse(bucket, startingPoint)
			map += (bucket -> (start + 1, end))
		}

		def addEnd(bucket: Int) {
			val (start, end) = map.getOrElse(bucket, startingPoint)
			map += (bucket -> (start, end + 1))
		}

		def compute(buckets: List[Int]): List[IntVsTimePlot] = {

			val (_, _, sessions) = buckets.foldLeft(0, 0, List.empty[IntVsTimePlot]) { (accumulator, bucket) =>
				val (previousSessions, previousEnds, sessions) = accumulator
				val (bucketStarts, bucketEnds) = map.getOrElse(bucket, startingPoint)
				val bucketSessions = previousSessions - previousEnds + bucketStarts
				(bucketSessions, bucketEnds, IntVsTimePlot(bucket, bucketSessions) :: sessions)
			}

			sessions.reverse
		}
	}
}
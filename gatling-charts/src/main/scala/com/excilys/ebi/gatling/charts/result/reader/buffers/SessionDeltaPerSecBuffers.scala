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

object SessionDeltas {
    val empty = SessionDeltas(0, 0)
}

case class SessionDeltas(starts: Int, ends: Int) {
	
	def addStart = copy(starts = starts + 1)
	def addEnd = copy(ends = ends + 1)
}

class SessionDeltaBuffer {
	
	val map = mutable.HashMap.empty[Int, SessionDeltas].withDefaultValue(SessionDeltas.empty)

	def addStart(bucket: Int) {
		val deltas = map(bucket)
		map += (bucket -> deltas.addStart)
	}

	def addEnd(bucket: Int) {
		val delta = map(bucket)
		map += (bucket -> delta.addEnd)
	}

	def compute(buckets: Seq[Int]): Vector[IntVsTimePlot] = {

		val (_, _, sessions) = buckets.foldLeft(0, 0, Vector.empty[IntVsTimePlot]) { (accumulator, bucket) =>
			val (previousSessions, previousEnds, sessions) = accumulator
			val delta = map(bucket)
			val bucketSessions = previousSessions - previousEnds + delta.starts
			(bucketSessions, delta.ends, sessions :+ IntVsTimePlot(bucket, bucketSessions))
		}

		sessions
	}
}

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

}
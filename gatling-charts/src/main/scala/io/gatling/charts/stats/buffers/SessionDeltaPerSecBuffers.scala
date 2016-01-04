/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.charts.stats.buffers

import scala.collection.mutable
import io.gatling.charts.stats.UserRecord
import io.gatling.core.stats.IntVsTimePlot
import io.gatling.core.stats.message.{ End, Start }

private[stats] object SessionDeltas {
  val Empty = SessionDeltas(0, 0)
}

private[stats] case class SessionDeltas(starts: Int, ends: Int) {

  def addStart() = copy(starts = starts + 1)
  def addEnd() = copy(ends = ends + 1)
}

private[stats] class SessionDeltaBuffer(buckets: Array[Int]) {

  val startCounts: Array[Int] = Array.fill(buckets.length)(0)
  val endCounts: Array[Int] = Array.fill(buckets.length)(0)

  def addStart(bucket: Int): Unit = startCounts(bucket) = startCounts(bucket) + 1

  def addEnd(bucket: Int): Unit = endCounts(bucket) = endCounts(bucket) + 1

  def distribution: List[IntVsTimePlot] =
    buckets.view.zipWithIndex.foldLeft(List.empty[IntVsTimePlot]) { (activeSessions, timeAndBucketNumber) =>
      val (time, bucketNumber) = timeAndBucketNumber
      val previousSessions = if (activeSessions.isEmpty) 0 else activeSessions.head.value
      val previousEnds = if (bucketNumber == 0) 0 else endCounts(bucketNumber - 1)
      val bucketSessions = previousSessions - previousEnds + startCounts(bucketNumber)
      IntVsTimePlot(time, bucketSessions) :: activeSessions
    }.reverse
}

private[stats] trait SessionDeltaPerSecBuffers {
  this: Buckets =>

  val sessionDeltaPerSecBuffers: mutable.Map[Option[String], SessionDeltaBuffer] = mutable.Map.empty
  val orphanStartRecords = mutable.Map.empty[String, UserRecord]

  def getSessionDeltaPerSecBuffers(scenarioName: Option[String]): SessionDeltaBuffer = sessionDeltaPerSecBuffers.getOrElseUpdate(scenarioName, new SessionDeltaBuffer(buckets))

  def addSessionBuffers(record: UserRecord): Unit = {
    record.event match {
      case Start =>
        getSessionDeltaPerSecBuffers(None).addStart(record.startBucket)
        getSessionDeltaPerSecBuffers(Some(record.scenario)).addStart(record.startBucket)
        orphanStartRecords += record.userId -> record

      case End =>
        getSessionDeltaPerSecBuffers(None).addEnd(record.endBucket)
        getSessionDeltaPerSecBuffers(Some(record.scenario)).addEnd(record.endBucket)
        orphanStartRecords -= record.userId
    }
  }

  def endOrphanUserRecords(endDateBucket: Int): Unit =
    orphanStartRecords.values.foreach { start =>
      getSessionDeltaPerSecBuffers(None).addEnd(endDateBucket)
      getSessionDeltaPerSecBuffers(Some(start.scenario)).addEnd(endDateBucket)
    }
}

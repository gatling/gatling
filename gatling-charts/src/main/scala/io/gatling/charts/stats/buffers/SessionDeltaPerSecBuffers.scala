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

import io.gatling.commons.util.Maps._
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

private[stats] class SessionDeltaBuffer(minTimestamp: Long, maxTimestamp: Long, buckets: Array[Int]) {

  private val runDurationInSeconds = math.ceil((maxTimestamp - minTimestamp) / 1000.0).toInt
  private val startCounts: Array[Int] = Array.fill(runDurationInSeconds)(0)
  private val endCounts: Array[Int] = Array.fill(runDurationInSeconds)(0)

  def addStart(bucket: Int): Unit = startCounts(bucket) += 1

  def addEnd(bucket: Int): Unit = endCounts(bucket) += 1

  private val bucketWidthInMillis = ((maxTimestamp - minTimestamp) / buckets.length).toInt
  private def secondToBucket(second: Int): Int = math.min(second * 1000 / bucketWidthInMillis, buckets.length - 1)

  def distribution: List[IntVsTimePlot] = {

    val eachSecondActiveSessions = Array.fill(runDurationInSeconds)(0)

    for { second <- 0 until runDurationInSeconds } {
      val previousSessions = if (second == 0) 0 else eachSecondActiveSessions(second - 1)
      val previousEnds = if (second == 0) 0 else endCounts(second - 1)
      val bucketSessions = previousSessions - previousEnds + startCounts(second)
      eachSecondActiveSessions.update(second, bucketSessions)
    }

    eachSecondActiveSessions.zipWithIndex.iterator
      .map { case (sessions, second) => second -> sessions }
      .groupByKey(secondToBucket)
      .map {
        case (bucket, sessionCounts) =>
          val averageSessionCount = sessionCounts.sum / sessionCounts.size
          val time = buckets(bucket)
          IntVsTimePlot(time, averageSessionCount)
      }.toList.sortBy(_.time)
  }
}

private[stats] trait SessionDeltaPerSecBuffers {
  this: Buckets with RunTimes =>

  private val sessionDeltaPerSecBuffers = mutable.Map.empty[Option[String], SessionDeltaBuffer]
  private val orphanStartRecords = mutable.Map.empty[String, UserRecord]

  def getSessionDeltaPerSecBuffers(scenarioName: Option[String]): SessionDeltaBuffer =
    sessionDeltaPerSecBuffers.getOrElseUpdate(scenarioName, new SessionDeltaBuffer(minTimestamp, maxTimestamp, buckets))

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

  def endOrphanUserRecords(): Unit =
    orphanStartRecords.values.foreach { start =>
      getSessionDeltaPerSecBuffers(None).addEnd(buckets.length - 1)
      getSessionDeltaPerSecBuffers(Some(start.scenario)).addEnd(buckets.length - 1)
    }
}

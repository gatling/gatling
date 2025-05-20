/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.charts.stats.{ IntVsTimePlot, UserRecord }
import io.gatling.core.stats.message.MessageEvent

private[stats] final class SessionCounters(minTimestamp: Long, maxTimestamp: Long, bucketToMillis: Array[Int]) {
  private val bucketWidthInMillis = ((maxTimestamp - minTimestamp) / bucketToMillis.length).toInt

  private var currentBuffer = 0
  private val startCounts: Array[Int] = Array.fill(bucketToMillis.length)(0)
  private val concurrentUsers: Array[Int] = Array.fill(bucketToMillis.length)(0)
  private val maxConcurrentUsers: Array[Int] = Array.fill(bucketToMillis.length)(0)

  // assume timestamps are always moving forward
  private def updateCurrentBucket(second: Int): Unit = {
    val bucket = secondToBucket(second)
    if (bucket > currentBuffer) {
      // moving currentBuffer forward, initialize all buffers between old and new position
      for (i <- currentBuffer + 1 to bucket) {
        concurrentUsers(i) = concurrentUsers(currentBuffer)
        maxConcurrentUsers(i) = maxConcurrentUsers(currentBuffer)
      }
      currentBuffer = bucket
    }
  }

  def addStart(second: Int): Unit = {
    updateCurrentBucket(second)
    startCounts(currentBuffer) += 1
    concurrentUsers(currentBuffer) += 1
    if (concurrentUsers(currentBuffer) > maxConcurrentUsers(currentBuffer)) {
      maxConcurrentUsers(currentBuffer) = concurrentUsers(currentBuffer)
    }
  }

  def addEnd(second: Int): Unit = {
    updateCurrentBucket(second)
    concurrentUsers(currentBuffer) -= 1
  }

  private def secondToBucket(second: Int): Int = math.min(second * 1000 / bucketWidthInMillis, bucketToMillis.length - 1)

  def userStartRateSeries: List[IntVsTimePlot] =
    startCounts.zipWithIndex.map { case (startCount, bucket) =>
      new IntVsTimePlot(bucketToMillis(bucket), (startCount.toDouble * 1000 / bucketWidthInMillis).round.toInt)
    }.toList

  def maxConcurrentUsersSeries: List[IntVsTimePlot] =
    maxConcurrentUsers.zipWithIndex.map { case (max, bucket) =>
      new IntVsTimePlot(bucketToMillis(bucket), max)
    }.toList

  def flushTrailingConcurrentUsers(): Unit =
    for (i <- currentBuffer + 1 until bucketToMillis.length) {
      concurrentUsers(i) = concurrentUsers(currentBuffer)
      maxConcurrentUsers(i) = maxConcurrentUsers(currentBuffer)
    }
}

private[stats] trait SessionDeltaPerSecBuffers {
  this: Buckets with RunTimes =>

  private val sessionDeltaPerSecBuffers = mutable.Map.empty[Option[String], SessionCounters]

  def getSessionDeltaPerSecBuffers(scenarioName: Option[String]): SessionCounters =
    sessionDeltaPerSecBuffers.getOrElseUpdate(scenarioName, new SessionCounters(minTimestamp, maxTimestamp, buckets))

  private def timestamp2SecondOffset(timestamp: Long) = {
    val millisOffset = timestamp - minTimestamp
    val includeRightBorderCorrection =
      if (millisOffset > 0 && millisOffset % 1000 == 0) {
        1
      } else {
        0
      }

    (millisOffset / 1000).toInt - includeRightBorderCorrection
  }

  def addSessionBuffers(record: UserRecord): Unit =
    record.event match {
      case MessageEvent.Start =>
        val startSecond = timestamp2SecondOffset(record.timestamp)
        getSessionDeltaPerSecBuffers(None).addStart(startSecond)
        getSessionDeltaPerSecBuffers(Some(record.scenario)).addStart(startSecond)

      case MessageEvent.End =>
        val endSecond = timestamp2SecondOffset(record.timestamp)
        getSessionDeltaPerSecBuffers(None).addEnd(endSecond)
        getSessionDeltaPerSecBuffers(Some(record.scenario)).addEnd(endSecond)
    }

  def flushTrailingConcurrentUsers(): Unit =
    sessionDeltaPerSecBuffers.values.foreach(_.flushTrailingConcurrentUsers())
}

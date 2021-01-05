/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.util.concurrent.atomic.LongAdder

import scala.collection.mutable

import io.gatling.charts.stats.{ IntVsTimePlot, UserRecord }
import io.gatling.core.stats.message.MessageEvent

private[stats] object SessionDeltas {
  val Empty: SessionDeltas = SessionDeltas(0, 0)
}

private[stats] final case class SessionDeltas(starts: Int, ends: Int)

private[stats] class SessionDeltaBuffer(minTimestamp: Long, maxTimestamp: Long, buckets: Array[Int], runDurationInSeconds: Int) {

  private val startCounts: Array[Int] = Array.fill(runDurationInSeconds)(0)
  private val endCounts: Array[Int] = Array.fill(runDurationInSeconds)(0)

  def addStart(second: Int): Unit = startCounts(second) += 1

  def addEnd(second: Int): Unit = endCounts(second) += 1

  def endDandling(): Unit = addEnd(runDurationInSeconds - 1)

  private val bucketWidthInMillis = ((maxTimestamp - minTimestamp) / buckets.length).toInt
  private def secondToBucket(second: Int): Int = math.min(second * 1000 / bucketWidthInMillis, buckets.length - 1)

  def distribution: List[IntVsTimePlot] = {

    val eachSecondActiveSessions = Array.fill(runDurationInSeconds)(0)

    for (second <- 0 until runDurationInSeconds) {
      val previousSessions = if (second == 0) 0 else eachSecondActiveSessions(second - 1)
      val previousEnds = if (second == 0) 0 else endCounts(second - 1)
      val bucketSessions = previousSessions - previousEnds + startCounts(second)
      eachSecondActiveSessions.update(second, bucketSessions)
    }

    eachSecondActiveSessions.zipWithIndex.view
      .groupMap { case (_, second) => secondToBucket(second) } { case (sessions, _) => sessions }
      .map { case (bucket, sessionCounts) =>
        val averageSessionCount = sessionCounts.sum / sessionCounts.size
        val time = buckets(bucket)
        new IntVsTimePlot(time, averageSessionCount)
      }
      .to(List)
      .sortBy(_.time)
  }
}

private[stats] trait SessionDeltaPerSecBuffers {
  this: Buckets with RunTimes =>

  private val sessionDeltaPerSecBuffers = mutable.Map.empty[Option[String], SessionDeltaBuffer]
  private val userCountByScenario = mutable.Map.empty[String, LongAdder]
  private val runDurationInSeconds = math.ceil((maxTimestamp - minTimestamp) / 1000.0).toInt

  def getSessionDeltaPerSecBuffers(scenarioName: Option[String]): SessionDeltaBuffer =
    sessionDeltaPerSecBuffers.getOrElseUpdate(scenarioName, new SessionDeltaBuffer(minTimestamp, maxTimestamp, buckets, runDurationInSeconds))

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

  def addSessionBuffers(record: UserRecord): Unit = {
    record.event match {
      case MessageEvent.Start =>
        val startSecond = timestamp2SecondOffset(record.timestamp)
        getSessionDeltaPerSecBuffers(None).addStart(startSecond)
        getSessionDeltaPerSecBuffers(Some(record.scenario)).addStart(startSecond)
        userCountByScenario.getOrElseUpdate(record.scenario, new LongAdder).increment()

      case MessageEvent.End =>
        val endSecond = timestamp2SecondOffset(record.timestamp)
        getSessionDeltaPerSecBuffers(None).addEnd(endSecond)
        getSessionDeltaPerSecBuffers(Some(record.scenario)).addEnd(endSecond)
        userCountByScenario.getOrElseUpdate(record.scenario, new LongAdder).decrement()
    }
  }

  def endDandlingStartedUser(): Unit =
    for {
      (scenario, count) <- userCountByScenario
      sum = count.sum().toInt
      _ <- 0 until sum
    } {
      getSessionDeltaPerSecBuffers(None).endDandling()
      getSessionDeltaPerSecBuffers(Some(scenario)).endDandling()
    }
}

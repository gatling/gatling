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

import scala.collection.mutable

import io.gatling.charts.stats.{ GroupRecord, IntVsTimePlot, RequestRecord }
import io.gatling.commons.shared.unstable.model.stats.{ GeneralStats, Group }
import io.gatling.commons.stats.Status

import com.tdunning.math.stats.AVLTreeDigest

private[stats] abstract class GeneralStatsBuffers(durationInSec: Long) {

  val requestGeneralStatsBuffers = mutable.Map.empty[BufferKey, GeneralStatsBuffer]
  val groupDurationGeneralStatsBuffers = mutable.Map.empty[BufferKey, GeneralStatsBuffer]
  val groupCumulatedResponseTimeGeneralStatsBuffers = mutable.Map.empty[BufferKey, GeneralStatsBuffer]
  val requestCounts = mutable.Map.empty[BufferKey, (Int, Int)]

  def getRequestGeneralStatsBuffers(request: Option[String], group: Option[Group], status: Option[Status]): GeneralStatsBuffer =
    requestGeneralStatsBuffers.getOrElseUpdate(BufferKey(request, group, status), new GeneralStatsBuffer(durationInSec))

  def getGroupDurationGeneralStatsBuffers(group: Group, status: Option[Status]): GeneralStatsBuffer =
    groupDurationGeneralStatsBuffers.getOrElseUpdate(BufferKey(None, Some(group), status), new GeneralStatsBuffer(durationInSec))

  def getGroupCumulatedResponseTimeGeneralStatsBuffers(group: Group, status: Option[Status]): GeneralStatsBuffer =
    groupCumulatedResponseTimeGeneralStatsBuffers.getOrElseUpdate(BufferKey(None, Some(group), status), new GeneralStatsBuffer(durationInSec))

  def updateRequestGeneralStatsBuffers(record: RequestRecord): Unit = {
    import record._
    getRequestGeneralStatsBuffers(Some(name), group, None).update(responseTime)
    getRequestGeneralStatsBuffers(Some(name), group, Some(status)).update(responseTime)

    getRequestGeneralStatsBuffers(None, None, None).update(responseTime)
    getRequestGeneralStatsBuffers(None, None, Some(status)).update(responseTime)
  }

  def updateGroupGeneralStatsBuffers(record: GroupRecord): Unit = {
    import record._
    getGroupCumulatedResponseTimeGeneralStatsBuffers(group, None).update(cumulatedResponseTime)
    getGroupCumulatedResponseTimeGeneralStatsBuffers(group, Some(status)).update(cumulatedResponseTime)
    getGroupDurationGeneralStatsBuffers(group, None).update(duration)
    getGroupDurationGeneralStatsBuffers(group, Some(status)).update(duration)
  }
}

private[stats] class GeneralStatsBuffer(durationInSec: Long) {

  val counts = mutable.Map.empty[Int, Int]
  val digest = new AVLTreeDigest(100.0)
  var sumOfSquares = 0L
  var sum = 0L

  def update(time: Int): Unit = {

    val newCount = counts.get(time) match {
      case Some(count) => count + 1
      case None        => 1
    }
    counts.put(time, newCount)

    digest.add(time)
    sumOfSquares += time.toLong * time.toLong
    sum += time
  }

  lazy val stats: GeneralStats = {
    val valuesCount = digest.size
    if (valuesCount == 0) {
      GeneralStats.NoPlot

    } else {
      val count = digest.size
      val mean = sum.toDouble / count
      val stdDev = math.sqrt(sumOfSquares.toDouble / count - math.pow(mean, 2))
      val meanRequestsPerSec = valuesCount.toDouble / durationInSec

      val min = digest.quantile(0).toInt
      val max = digest.quantile(1).toInt

      val percentile: Double => Int = (rank: Double) => math.round(digest.quantile(rank / 100.0)).toInt

      GeneralStats(min.toInt, max.toInt, valuesCount, math.round(mean).toInt, math.round(stdDev).toInt, percentile, meanRequestsPerSec)
    }
  }

  def distribution: Iterable[IntVsTimePlot] = counts.map { case (time, count) => new IntVsTimePlot(time, count) }
}

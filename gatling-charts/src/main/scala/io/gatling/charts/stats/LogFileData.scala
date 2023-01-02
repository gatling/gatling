/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.charts.stats

import scala.collection.immutable.ArraySeq

import io.gatling.charts.stats.buffers.{ CountsBuffer, GeneralStatsBuffer, PercentilesBuffers }
import io.gatling.commons.shared.unstable.model.stats._
import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.stats.assertion.Assertion

private[gatling] final class LogFileData(
    val runInfo: RunInfo,
    resultsHolder: ResultsHolder,
    step: Double
) extends GeneralStatsSource {
  private val secMillisecRatio: Double = 1000.0

  override def assertions: List[Assertion] = runInfo.assertions

  override val statsPaths: List[StatsPath] =
    resultsHolder.groupAndRequestsNameBuffer.map.toList
      .map {
        case (path @ RequestStatsPath(_, group), time) => (path, (time, group.map(_.hierarchy.size + 1).getOrElse(0)))
        case (path @ GroupStatsPath(group), time)      => (path, (time, group.hierarchy.size))
        case _                                         => throw new UnsupportedOperationException
      }
      .sortBy(_._2)
      .map(_._1)

  def scenarioNames: List[String] =
    resultsHolder.scenarioNameBuffer.map.toList
      .sortBy(_._2)
      .map(_._1)

  def numberOfActiveSessionsPerSecond(scenarioName: Option[String]): Seq[IntVsTimePlot] =
    resultsHolder
      .getSessionDeltaPerSecBuffers(scenarioName)
      .distribution

  private def toNumberPerSec(value: Int) = (value / step * secMillisecRatio).round.toInt

  private def countBuffer2IntVsTimePlots(buffer: CountsBuffer): Seq[CountsVsTimePlot] =
    buffer.distribution
      .map(plot => new CountsVsTimePlot(plot.time, toNumberPerSec(plot.oks), kos = toNumberPerSec(plot.kos)))
      .toSeq
      .sortBy(_.time)

  def numberOfRequestsPerSecond(requestName: Option[String], group: Option[Group]): Seq[CountsVsTimePlot] =
    countBuffer2IntVsTimePlots(resultsHolder.getRequestsPerSecBuffer(requestName, group))

  def numberOfResponsesPerSecond(requestName: Option[String], group: Option[Group]): Seq[CountsVsTimePlot] =
    countBuffer2IntVsTimePlots(resultsHolder.getResponsesPerSecBuffer(requestName, group))

  private def distribution(
      maxPlots: Int,
      allBuffer: GeneralStatsBuffer,
      okBuffers: GeneralStatsBuffer,
      koBuffer: GeneralStatsBuffer
  ): (Seq[PercentVsTimePlot], Seq[PercentVsTimePlot]) = {
    // get main and max for request/all status
    val size = allBuffer.stats.count
    val ok = okBuffers.distribution
    val ko = koBuffer.distribution
    val min = allBuffer.stats.min
    val max = allBuffer.stats.max

    def percent(s: Int) = s * 100.0 / size

    if (max - min <= maxPlots) {
      // use exact values
      def plotsToPercents(plots: Iterable[IntVsTimePlot]) = plots.map(plot => new PercentVsTimePlot(plot.time, percent(plot.value))).toSeq.sortBy(_.time)
      (plotsToPercents(ok), plotsToPercents(ko))
    } else {
      // use buckets
      val step = StatsHelper.step(min, max, maxPlots)
      val buckets = StatsHelper.buckets(min, max, step)

      val halfStep = step / 2
      val bucketFunction = (t: Int) => {
        val value = t min (max - 1)
        (value - (value - min) % step + halfStep).round.toInt
      }

      def process(buffer: Iterable[IntVsTimePlot]): Seq[PercentVsTimePlot] = {
        val bucketsWithValues: Map[Int, Double] =
          buffer
            .groupMapReduce(record => bucketFunction(record.time))(record => percent(record.value))(_ + _)

        ArraySeq.unsafeWrapArray(buckets).map { bucket =>
          new PercentVsTimePlot(bucket, bucketsWithValues.getOrElse(bucket, 0.0))
        }
      }

      (process(ok), process(ko))
    }
  }

  def responseTimeDistribution(maxPlots: Int, requestName: Option[String], group: Option[Group]): (Seq[PercentVsTimePlot], Seq[PercentVsTimePlot]) =
    distribution(
      maxPlots,
      resultsHolder.getRequestGeneralStatsBuffers(requestName, group, None),
      resultsHolder.getRequestGeneralStatsBuffers(requestName, group, Some(OK)),
      resultsHolder.getRequestGeneralStatsBuffers(requestName, group, Some(KO))
    )

  def groupCumulatedResponseTimeDistribution(maxPlots: Int, group: Group): (Seq[PercentVsTimePlot], Seq[PercentVsTimePlot]) =
    distribution(
      maxPlots,
      resultsHolder.getGroupCumulatedResponseTimeGeneralStatsBuffers(group, None),
      resultsHolder.getGroupCumulatedResponseTimeGeneralStatsBuffers(group, Some(OK)),
      resultsHolder.getGroupCumulatedResponseTimeGeneralStatsBuffers(group, Some(KO))
    )

  def groupDurationDistribution(maxPlots: Int, group: Group): (Seq[PercentVsTimePlot], Seq[PercentVsTimePlot]) =
    distribution(
      maxPlots,
      resultsHolder.getGroupDurationGeneralStatsBuffers(group, None),
      resultsHolder.getGroupDurationGeneralStatsBuffers(group, Some(OK)),
      resultsHolder.getGroupDurationGeneralStatsBuffers(group, Some(KO))
    )

  def requestGeneralStats(requestName: Option[String], group: Option[Group], status: Option[Status]): GeneralStats =
    resultsHolder
      .getRequestGeneralStatsBuffers(requestName, group, status)
      .stats

  def groupCumulatedResponseTimeGeneralStats(group: Group, status: Option[Status]): GeneralStats =
    resultsHolder
      .getGroupCumulatedResponseTimeGeneralStatsBuffers(group, status)
      .stats

  def groupDurationGeneralStats(group: Group, status: Option[Status]): GeneralStats =
    resultsHolder
      .getGroupDurationGeneralStatsBuffers(group, status)
      .stats

  def numberOfRequestInResponseTimeRange(requestName: Option[String], group: Option[Group]): Seq[(String, String, Int)] = {
    val counts = resultsHolder.getResponseTimeRangeBuffers(requestName, group)
    val lowerBound = resultsHolder.lowerBound
    val higherBound = resultsHolder.higherBound

    List(
      (s"t < $lowerBound ms", s"t < $lowerBound ms", counts.low),
      (s"$lowerBound ms <= t < $higherBound ms", s"t >= $lowerBound ms <br> t < $higherBound ms", counts.middle),
      (s"t >= $higherBound ms", s"t >= $higherBound ms", counts.high),
      ("failed", "failed", counts.ko)
    )
  }

  def responseTimePercentilesOverTime(status: Status, requestName: Option[String], group: Option[Group]): Iterable[PercentilesVsTimePlot] =
    resultsHolder.getResponseTimePercentilesBuffers(requestName, group, status).percentiles

  private def timeAgainstGlobalNumberOfRequestsPerSec(buffer: PercentilesBuffers): Seq[IntVsTimePlot] = {
    val globalCountsByBucket = resultsHolder.getRequestsPerSecBuffer(None, None).counts

    buffer.digests.view.zipWithIndex
      .collect { case (Some(digest), bucketNumber) =>
        val count = globalCountsByBucket(bucketNumber)
        new IntVsTimePlot(toNumberPerSec(count.total), digest.quantile(0.95).toInt)
      }
      .to(Seq)
      .sortBy(_.time)
  }

  def responseTimeAgainstGlobalNumberOfRequestsPerSec(status: Status, requestName: String, group: Option[Group]): Seq[IntVsTimePlot] = {
    val percentilesBuffer = resultsHolder.getResponseTimePercentilesBuffers(Some(requestName), group, status)
    timeAgainstGlobalNumberOfRequestsPerSec(percentilesBuffer)
  }

  def groupCumulatedResponseTimePercentilesOverTime(status: Status, group: Group): Iterable[PercentilesVsTimePlot] =
    resultsHolder.getGroupCumulatedResponseTimePercentilesBuffers(group, status).percentiles

  def groupDurationPercentilesOverTime(status: Status, group: Group): Iterable[PercentilesVsTimePlot] =
    resultsHolder.getGroupDurationPercentilesBuffers(group, status).percentiles

  def errors(requestName: Option[String], group: Option[Group]): Seq[ErrorStats] = {
    val buff = resultsHolder.getErrorsBuffers(requestName, group)
    val total = buff.foldLeft(0)(_ + _._2)
    buff.toSeq.map { case (name, count) => new ErrorStats(name, count, total) }.sortWith(_.count > _.count)
  }
}

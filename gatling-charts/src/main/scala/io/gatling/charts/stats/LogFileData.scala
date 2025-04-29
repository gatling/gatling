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

package io.gatling.charts.stats

import scala.collection.immutable.ArraySeq

import io.gatling.charts.stats.buffers.{ CountsBuffer, GeneralStatsBuffer, PercentilesBuffers }
import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.stats.assertion.Assertion
import io.gatling.core.stats.ErrorStats
import io.gatling.shared.model.assertion.AssertionStatsRepository

private[gatling] final class LogFileData(
    val runInfo: RunInfo,
    resultsHolder: ResultsHolder,
    step: Double
) extends AssertionStatsRepository {
  private val secMillisecRatio: Double = 1000.0

  //// BEGIN AssertionStatsRepository
  override def allRequestPaths(): List[AssertionStatsRepository.StatsPath.Request] =
    resultsHolder.groupAndRequestsNameBuffer.map.toList
      .collect { case (RequestStatsPath(request, group), time) =>
        val path = AssertionStatsRepository.StatsPath.Request(group.map(_.hierarchy).getOrElse(Nil), request)
        val depth = group.map(_.hierarchy.size + 1).getOrElse(0)
        (path, (time, depth))
      }
      .sortBy(_._2)
      .map(_._1)

  override def findPathByParts(parts: List[String]): Option[AssertionStatsRepository.StatsPath] =
    resultsHolder.groupAndRequestsNameBuffer.map.keys.collectFirst {
      case RequestStatsPath(request, group) if group.map(_.hierarchy).getOrElse(Nil) ::: request :: Nil == parts =>
        AssertionStatsRepository.StatsPath.Request(group.map(_.hierarchy).getOrElse(Nil), request)
      case GroupStatsPath(group) if group.hierarchy == parts => AssertionStatsRepository.StatsPath.Group(group.hierarchy)
    }

  private def toAssertionStats(generalStats: Option[GeneralStats]): AssertionStatsRepository.Stats =
    generalStats match {
      case Some(stats) =>
        AssertionStatsRepository.Stats(
          min = stats.min,
          max = stats.max,
          count = stats.count,
          mean = stats.mean,
          stdDev = stats.stdDev,
          percentile = stats.percentile,
          meanRequestsPerSec = stats.meanRequestsPerSec
        )
      case _ =>
        AssertionStatsRepository.Stats.NoData
    }

  override def requestGeneralStats(group: List[String], request: Option[String], status: Option[Status]): AssertionStatsRepository.Stats =
    toAssertionStats(requestGeneralStats(request, if (group.nonEmpty) Some(Group(group)) else None, status))

  override def groupCumulatedResponseTimeGeneralStats(group: List[String], status: Option[Status]): AssertionStatsRepository.Stats =
    toAssertionStats(groupCumulatedResponseTimeGeneralStats(Group(group), status))

  //// END AssertionStatsRepository

  def assertions: List[Assertion] = runInfo.assertions

  val statsPaths: List[StatsPath] =
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
  ): (Seq[PercentVsTimePlot], Seq[PercentVsTimePlot]) =
    allBuffer.stats match {
      case Some(stats) =>
        val size = stats.count
        val min = stats.min
        val max = stats.max
        val ok = okBuffers.distribution
        val ko = koBuffer.distribution

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

      case _ => (Nil, Nil)
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

  def requestGeneralStats(requestName: Option[String], group: Option[Group], status: Option[Status]): Option[GeneralStats] =
    resultsHolder
      .getRequestGeneralStatsBuffers(requestName, group, status)
      .stats

  def groupCumulatedResponseTimeGeneralStats(group: Group, status: Option[Status]): Option[GeneralStats] =
    resultsHolder
      .getGroupCumulatedResponseTimeGeneralStatsBuffers(group, status)
      .stats

  def groupDurationGeneralStats(group: Group, status: Option[Status]): Option[GeneralStats] =
    resultsHolder
      .getGroupDurationGeneralStatsBuffers(group, status)
      .stats

  def numberOfRequestInResponseTimeRanges(requestName: Option[String], group: Option[Group]): Ranges = {
    val counts = resultsHolder.getResponseTimeRangeBuffers(requestName, group)
    Ranges(
      lowerBound = resultsHolder.lowerBound,
      higherBound = resultsHolder.higherBound,
      lowCount = counts.low,
      middleCount = counts.middle,
      highCount = counts.high,
      koCount = counts.ko
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

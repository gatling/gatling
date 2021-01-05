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

package io.gatling.charts.stats

import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Base64

import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.io.Source

import io.gatling.charts.stats.buffers.{ CountsBuffer, GeneralStatsBuffer, PercentilesBuffers }
import io.gatling.commons.shared.unstable.model.stats.{ ErrorStats, GeneralStats, GeneralStatsSource, Group, GroupStatsPath, RequestStatsPath, StatsPath }
import io.gatling.commons.shared.unstable.util.PathHelper._
import io.gatling.commons.stats._
import io.gatling.commons.stats.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingFiles.simulationLogDirectory
import io.gatling.core.stats.message.MessageEvent
import io.gatling.core.stats.writer._

import boopickle.Default._
import com.typesafe.scalalogging.StrictLogging

object LogFileReader {

  private val LogStep = 100000
  private val SecMillisecRatio = 1000.0
  private val SimulationFilesNamePattern = """.*\.log"""

  private final case class FirstPassData(runStart: Long, runEnd: Long, runMessage: RunMessage, assertions: List[Assertion])
}

private[gatling] class LogFileReader(runUuid: String)(implicit configuration: GatlingConfiguration) extends GeneralStatsSource with StrictLogging {

  import LogFileReader._

  private val inputFiles = simulationLogDirectory(runUuid, create = false, configuration).files
    .collect { case file if file.filename.matches(SimulationFilesNamePattern) => file.path }

  logger.info(s"Collected $inputFiles from $runUuid")
  require(inputFiles.nonEmpty, "simulation directory doesn't contain any log file.")

  private def parseInputFiles[T](f: Iterator[String] => T): T = {

    def multipleFileIterator(streams: Seq[InputStream]): Iterator[String] =
      streams.map(Source.fromInputStream(_)(configuration.core.charset).getLines()).reduce((first, second) => first ++ second)

    val streams = inputFiles.map(_.inputStream)
    try f(multipleFileIterator(streams))
    finally streams.foreach(_.close)
  }

  private def firstPass(records: Iterator[String]): FirstPassData = {

    logger.info("First pass")

    var count = 0

    var runStart = Long.MaxValue
    var runEnd = Long.MinValue

    def updateRunStart(eventStart: Long): Unit =
      runStart = math.min(runStart, eventStart)

    def updateRunEnd(eventEnd: Long): Unit =
      runEnd = math.max(runEnd, eventEnd)

    val runMessages = mutable.ListBuffer.empty[RunMessage]
    val assertions = mutable.LinkedHashSet.empty[Assertion]

    records.foreach { line =>
      count += 1
      if (count % LogStep == 0) logger.info(s"First pass, read $count lines")

      line.split(DataWriterMessageSerializer.Separator) match {

        case RawRequestRecord(array) =>
          updateRunStart(array(3).toLong)
          updateRunEnd(array(4).toLong)

        case RawUserRecord(array) =>
          val timestamp = array(3).toLong
          if (array(2) == MessageEvent.Start.name) {
            updateRunStart(timestamp)
          } else {
            updateRunEnd(timestamp)
          }

        case RawGroupRecord(array) =>
          updateRunStart(array(2).toLong)
          updateRunEnd(array(3).toLong)

        case RawRunRecord(array) =>
          runMessages += RunMessage(array(1), array(2), array(3).toLong, array(4).trim, array(5).trim)

        case RawAssertionRecord(array) =>
          val assertion: Assertion = {
            // WARN: don't believe IntelliJ here, this import is absolutely mandatory, see
            import io.gatling.commons.stats.assertion.AssertionPicklers._
            val base64String = array(1)
            val bytes = Base64.getDecoder.decode(base64String)
            Unpickle[Assertion].fromBytes(ByteBuffer.wrap(bytes))
          }

          assertions += assertion

        case RawErrorRecord(_) =>
        case _ =>
          logger.debug(s"Record broken on line $count: $line")
      }
    }

    logger.info(s"First pass done: read $count lines")

    FirstPassData(
      runStart,
      runEnd,
      runMessages.headOption.getOrElse(throw new UnsupportedOperationException(s"Files $inputFiles don't contain any valid run record")),
      assertions.toList
    )
  }

  private val firstPassData = parseInputFiles(firstPass)
  val runStart: Long = firstPassData.runStart
  val runEnd: Long = firstPassData.runEnd
  val runMessage: RunMessage = firstPassData.runMessage
  override val assertions: List[Assertion] = firstPassData.assertions

  private val step = StatsHelper.step(
    math.floor(runStart / SecMillisecRatio).toInt,
    math.ceil(runEnd / SecMillisecRatio).toInt,
    configuration.charting.maxPlotsPerSeries
  ) * SecMillisecRatio

  private val buckets = StatsHelper.buckets(0, runEnd - runStart, step)
  private val bucketFunction = StatsHelper.timeToBucketNumber(runStart, step, buckets.length)

  private def secondPass(records: Iterator[String]): ResultsHolder = {

    logger.info("Second pass")

    val resultsHolder = new ResultsHolder(runStart, runEnd, buckets)

    var count = 0

    val requestRecordParser = new RequestRecordParser(bucketFunction)
    val groupRecordParser = new GroupRecordParser(bucketFunction)

    records
      .foreach { line =>
        count += 1
        if (count % LogStep == 0) logger.info(s"Second pass, read $count lines")

        line.split(DataWriterMessageSerializer.Separator) match {
          case requestRecordParser(record) => resultsHolder.addRequestRecord(record)
          case groupRecordParser(record)   => resultsHolder.addGroupRecord(record)
          case UserRecordParser(record)    => resultsHolder.addUserRecord(record)
          case ErrorRecordParser(record)   => resultsHolder.addErrorRecord(record)
          case _                           =>
        }
      }

    resultsHolder.endDandlingStartedUser()

    logger.info(s"Second pass: read $count lines")

    resultsHolder
  }

  private val resultsHolder = parseInputFiles(secondPass)

  override val statsPaths: List[StatsPath] =
    resultsHolder.groupAndRequestsNameBuffer.map.toList
      .map {
        case (path @ RequestStatsPath(_, group), time) => (path, (time, group.map(_.hierarchy.size + 1).getOrElse(0)))
        case (path @ GroupStatsPath(group), time)      => (path, (time, group.hierarchy.size))
        case _                                         => throw new UnsupportedOperationException
      }
      .sortBy(_._2)
      .map(_._1)

  def requestNames: List[String] = statsPaths.collect { case RequestStatsPath(request, _) => request }

  def scenarioNames: List[String] =
    resultsHolder.scenarioNameBuffer.map.toList
      .sortBy(_._2)
      .map(_._1)

  def numberOfActiveSessionsPerSecond(scenarioName: Option[String]): Seq[IntVsTimePlot] =
    resultsHolder
      .getSessionDeltaPerSecBuffers(scenarioName)
      .distribution

  private def toNumberPerSec(value: Int) = (value / step * SecMillisecRatio).round.toInt

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

  def numberOfRequestInResponseTimeRange(requestName: Option[String], group: Option[Group]): Seq[(String, Int)] = {

    val counts = resultsHolder.getResponseTimeRangeBuffers(requestName, group)
    val lowerBound = configuration.charting.indicators.lowerBound
    val higherBound = configuration.charting.indicators.higherBound

    List(
      (s"t < $lowerBound ms", counts.low),
      (s"$lowerBound ms < t < $higherBound ms", counts.middle),
      (s"t > $higherBound ms", counts.high),
      ("failed", counts.ko)
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

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
package io.gatling.charts.stats

import java.io.InputStream
import java.nio.ByteBuffer

import scala.collection.{ breakOut, mutable }
import scala.io.Source

import io.gatling.charts.stats.buffers.{ CountsBuffer, GeneralStatsBuffer, PercentilesBuffers }
import io.gatling.commons.stats._
import io.gatling.commons.stats.assertion.Assertion
import io.gatling.commons.util.PathHelper._
import io.gatling.commons.util.StringHelper._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingFiles.simulationLogDirectory
import io.gatling.core.stats._
import io.gatling.core.stats.writer._

import boopickle.Default._
import com.typesafe.scalalogging.StrictLogging
import jodd.util.Base64

object LogFileReader {

  val LogStep = 100000
  val SecMillisecRatio = 1000.0
  val SimulationFilesNamePattern = """.*\.log"""
}

class LogFileReader(runUuid: String)(implicit configuration: GatlingConfiguration) extends GeneralStatsSource with StrictLogging {

  import LogFileReader._

  println("Parsing log file(s)...")

  val inputFiles = simulationLogDirectory(runUuid, create = false).files
    .collect { case file if file.filename.matches(SimulationFilesNamePattern) => file.path }

  logger.info(s"Collected $inputFiles from $runUuid")
  require(inputFiles.nonEmpty, "simulation directory doesn't contain any log file.")

  private def parseInputFiles[T](f: Iterator[String] => T): T = {

      def multipleFileIterator(streams: Seq[InputStream]): Iterator[String] =
        streams.map(Source.fromInputStream(_)(configuration.core.codec).getLines()).reduce((first, second) => first ++ second)

    val streams = inputFiles.map(_.inputStream)
    try f(multipleFileIterator(streams))
    finally streams.foreach(_.close)
  }

  case class FirstPassData(runStart: Long, runEnd: Long, runMessage: RunMessage, assertions: List[Assertion])

  private def firstPass(records: Iterator[String]): FirstPassData = {

    logger.info("First pass")

    var count = 0

    var runStart = Long.MaxValue
    var runEnd = Long.MinValue

      def updateRunLimits(eventStart: Long, eventEnd: Long): Unit = {
        runStart = math.min(runStart, eventStart)
        runEnd = math.max(runEnd, eventEnd)
      }

    val runMessages = mutable.ListBuffer.empty[RunMessage]
    val assertions = mutable.LinkedHashSet.empty[Assertion]

    records.foreach { line =>
      count += 1
      if (count % LogStep == 0) logger.info(s"First pass, read $count lines")

      line.split(LogFileDataWriter.Separator) match {

        case RawRequestRecord(array) =>
          updateRunLimits(array(5).toLong, array(6).toLong)

        case RawUserRecord(array) =>
          updateRunLimits(array(4).toLong, array(5).toLong)

        case RawGroupRecord(array) =>
          updateRunLimits(array(4).toLong, array(5).toLong)

        case RawRunRecord(array) =>
          runMessages += RunMessage(array(1), array(2).trimToOption, array(3), array(4).toLong, array(5).trim)

        case RawAssertionRecord(array) =>
          val assertion: Assertion = {
            val base64String = array(1)
            val bytes = Base64.decode(base64String)
            Unpickle[Assertion].fromBytes(ByteBuffer.wrap(bytes))
          }

          assertions += assertion

        case RawErrorRecord(array) =>

        case _ =>
          logger.debug(s"Record broken on line $count: $line")
      }
    }

    logger.info(s"First pass done: read $count lines")

    FirstPassData(runStart, runEnd, runMessages.head, assertions.toList)
  }

  val FirstPassData(runStart, runEnd, runMessage, assertions) = parseInputFiles(firstPass)

  val step = StatsHelper.step(math.floor(runStart / SecMillisecRatio).toInt, math.ceil(runEnd / SecMillisecRatio).toInt, configuration.charting.maxPlotsPerSeries) * SecMillisecRatio

  val buckets = StatsHelper.buckets(0, runEnd - runStart, step)
  val bucketFunction = StatsHelper.timeToBucketNumber(runStart, step, buckets.length)

  private def secondPass(records: Iterator[String]): ResultsHolder = {

    logger.info("Second pass")

    val resultsHolder = new ResultsHolder(runStart, runEnd, buckets)

    var count = 0

    val requestRecordParser = new RequestRecordParser(bucketFunction)
    val groupRecordParser = new GroupRecordParser(bucketFunction)
    val userRecordParser = new UserRecordParser(bucketFunction)

    records
      .foreach { line =>
        count += 1
        if (count % LogStep == 0) logger.info(s"Second pass, read $count lines")

        line.split(LogFileDataWriter.Separator) match {
          case requestRecordParser(record) => resultsHolder.addRequestRecord(record)
          case groupRecordParser(record)   => resultsHolder.addGroupRecord(record)
          case userRecordParser(record)    => resultsHolder.addUserRecord(record)
          case ErrorRecordParser(record)   => resultsHolder.addErrorRecord(record)
          case _                           =>
        }
      }

    resultsHolder.endOrphanUserRecords()

    logger.info(s"Second pass: read $count lines")

    resultsHolder
  }

  val resultsHolder = parseInputFiles(secondPass)

  println("Parsing log file(s) done")

  val statsPaths: List[StatsPath] =
    resultsHolder.groupAndRequestsNameBuffer.map.toList.map {
      case (path @ RequestStatsPath(request, group), time) => (path, (time, group.map(_.hierarchy.size + 1).getOrElse(0)))
      case (path @ GroupStatsPath(group), time) => (path, (time, group.hierarchy.size))
      case _ => throw new UnsupportedOperationException
    }.sortBy(_._2).map(_._1)

  def requestNames: List[String] = statsPaths.collect { case RequestStatsPath(request, _) => request }

  def scenarioNames: List[String] = resultsHolder.scenarioNameBuffer
    .map
    .toList
    .sortBy(_._2)
    .map(_._1)

  def numberOfActiveSessionsPerSecond(scenarioName: Option[String]): Seq[IntVsTimePlot] = resultsHolder
    .getSessionDeltaPerSecBuffers(scenarioName)
    .distribution

  private def toNumberPerSec(value: Int) = (value / step * SecMillisecRatio).round.toInt

  private def countBuffer2IntVsTimePlots(buffer: CountsBuffer): Seq[CountsVsTimePlot] =
    buffer
      .distribution
      .map(plot => plot.copy(oks = toNumberPerSec(plot.oks), kos = toNumberPerSec(plot.kos)))
      .toSeq
      .sortBy(_.time)

  def numberOfRequestsPerSecond(requestName: Option[String], group: Option[Group]): Seq[CountsVsTimePlot] =
    countBuffer2IntVsTimePlots(resultsHolder.getRequestsPerSecBuffer(requestName, group))

  def numberOfResponsesPerSecond(requestName: Option[String], group: Option[Group]): Seq[CountsVsTimePlot] =
    countBuffer2IntVsTimePlots(resultsHolder.getResponsesPerSecBuffer(requestName, group))

  private def distribution(maxPlots: Int, allBuffer: GeneralStatsBuffer, okBuffers: GeneralStatsBuffer, koBuffer: GeneralStatsBuffer): (Seq[PercentVsTimePlot], Seq[PercentVsTimePlot]) = {

    // get main and max for request/all status
    val size = allBuffer.stats.count
    val ok = okBuffers.distribution
    val ko = koBuffer.distribution
    val min = allBuffer.stats.min
    val max = allBuffer.stats.max

      def percent(s: Int) = s * 100.0 / size

    if (max - min <= maxPlots) {
        // use exact values
        def plotsToPercents(plots: Iterable[IntVsTimePlot]) = plots.map(plot => PercentVsTimePlot(plot.time, percent(plot.value))).toSeq.sortBy(_.time)
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

          val bucketsWithValues: Map[Int, Double] = buffer
            .map(record => (bucketFunction(record.time), record))
            .groupBy(_._1)
            .map {
              case (responseTimeBucket, recordList) =>

                val bucketSize = recordList.foldLeft(0) {
                  (partialSize, record) => partialSize + record._2.value
                }

                (responseTimeBucket, percent(bucketSize))
            }(breakOut)

          buckets.map {
            bucket => PercentVsTimePlot(bucket, bucketsWithValues.getOrElse(bucket, 0.0))
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

  def requestGeneralStats(requestName: Option[String], group: Option[Group], status: Option[Status]): GeneralStats = resultsHolder
    .getRequestGeneralStatsBuffers(requestName, group, status)
    .stats

  def groupCumulatedResponseTimeGeneralStats(group: Group, status: Option[Status]): GeneralStats = resultsHolder
    .getGroupCumulatedResponseTimeGeneralStatsBuffers(group, status)
    .stats

  def groupDurationGeneralStats(group: Group, status: Option[Status]): GeneralStats = resultsHolder
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

  private def timeAgainstGlobalNumberOfRequestsPerSec(buffer: PercentilesBuffers, status: Status, requestName: String, group: Option[Group]): Seq[IntVsTimePlot] = {

    val globalCountsByBucket = resultsHolder.getRequestsPerSecBuffer(None, None).counts

    buffer.digests.view.zipWithIndex
      .collect {
        case (Some(digest), bucketNumber) =>
          val count = globalCountsByBucket(bucketNumber)
          IntVsTimePlot(toNumberPerSec(count.total), digest.quantile(0.95).toInt)
      }
      .sortBy(_.time)
  }

  def responseTimeAgainstGlobalNumberOfRequestsPerSec(status: Status, requestName: String, group: Option[Group]): Seq[IntVsTimePlot] = {
    val percentilesBuffer = resultsHolder.getResponseTimePercentilesBuffers(Some(requestName), group, status)
    timeAgainstGlobalNumberOfRequestsPerSec(percentilesBuffer, status, requestName, group)
  }

  def groupCumulatedResponseTimePercentilesOverTime(status: Status, group: Group): Iterable[PercentilesVsTimePlot] =
    resultsHolder.getGroupCumulatedResponseTimePercentilesBuffers(group, status).percentiles

  def groupDurationPercentilesOverTime(status: Status, group: Group): Iterable[PercentilesVsTimePlot] =
    resultsHolder.getGroupDurationPercentilesBuffers(group, status).percentiles

  def errors(requestName: Option[String], group: Option[Group]): Seq[ErrorStats] = {
    val buff = resultsHolder.getErrorsBuffers(requestName, group)
    val total = buff.foldLeft(0)(_ + _._2)
    buff.toSeq.map { case (name, count) => ErrorStats(name, count, total) }.sortWith(_.count > _.count)
  }
}

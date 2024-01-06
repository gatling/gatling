/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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
import java.nio.file.{ Files, Path }
import java.util.Base64

import scala.collection.mutable
import scala.io.Source

import io.gatling.commons.stats.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingFiles.simulationLogDirectory
import io.gatling.core.stats.message.MessageEvent
import io.gatling.core.stats.writer._
import io.gatling.shared.util.PathHelper

import boopickle.Default._
import com.typesafe.scalalogging.StrictLogging

private[gatling] object LogFileReader extends StrictLogging {
  private val LogStep = 100000
  private val SecMillisecRatio: Double = 1000.0
  private val SimulationFilesNamePattern = """.*\.log"""

  def apply(runUuid: String, configuration: GatlingConfiguration): LogFileReader = {
    val inputFiles = PathHelper
      .deepFiles(simulationLogDirectory(runUuid, create = false, configuration.core.directory))
      .collect { case file if file.filename.matches(SimulationFilesNamePattern) => file.path }

    logger.info(s"Collected $inputFiles from $runUuid")
    require(inputFiles.nonEmpty, "simulation directory doesn't contain any log file.")

    new LogFileReader(inputFiles, configuration)
  }
}

private[gatling] final class LogFileReader(inputFiles: Seq[Path], configuration: GatlingConfiguration) extends StrictLogging {
  import LogFileReader._

  def read(): LogFileData = {
    val runInfo = parseInputFiles(firstPass)

    val step = StatsHelper.step(
      math.floor(runInfo.injectStart / SecMillisecRatio).toInt,
      math.ceil(runInfo.injectEnd / SecMillisecRatio).toInt,
      configuration.reports.maxPlotsPerSeries
    ) * SecMillisecRatio

    val buckets = StatsHelper.buckets(0, runInfo.injectEnd - runInfo.injectStart, step)
    val bucketFunction = StatsHelper.timeToBucketNumber(runInfo.injectStart, step, buckets.length)

    val resultsHolder = parseInputFiles(secondPass(runInfo, buckets, bucketFunction, _))

    new LogFileData(runInfo, resultsHolder, step)
  }

  private def parseInputFiles[T](f: Iterator[String] => T): T = {
    def multipleFileIterator(streams: Seq[InputStream]): Iterator[String] =
      streams.map(Source.fromInputStream(_)(configuration.core.charset).getLines()).reduce((first, second) => first ++ second)

    val streams = inputFiles.map(path => Files.newInputStream(path))
    try f(multipleFileIterator(streams))
    finally streams.foreach(_.close)
  }

  private def firstPass(records: Iterator[String]): RunInfo = {
    logger.info("First pass")

    var count = 0

    var injectStart = Long.MaxValue
    var injectEnd = Long.MinValue

    def updateInjectStart(eventStart: Long): Unit =
      injectStart = math.min(injectStart, eventStart)

    def updateInjectEnd(eventEnd: Long): Unit =
      injectEnd = math.max(injectEnd, eventEnd)

    val runMessages = mutable.ListBuffer.empty[RunMessage]
    val assertions = mutable.LinkedHashSet.empty[Assertion]

    records.foreach { line =>
      count += 1
      if (count % LogStep == 0) logger.info(s"First pass, read $count lines")

      line.split(DataWriterMessageSerializer.Separator) match {
        case RawRequestRecord(array) =>
          updateInjectStart(array(3).toLong)
          updateInjectEnd(array(4).toLong)

        case RawUserRecord(array) =>
          val timestamp = array(3).toLong
          if (array(2) == MessageEvent.Start.name) {
            updateInjectStart(timestamp)
          }
          updateInjectEnd(timestamp)

        case RawGroupRecord(array) =>
          updateInjectStart(array(2).toLong)
          updateInjectEnd(array(3).toLong)

        case RawRunRecord(array) =>
          runMessages += RunMessage(array(1), array(2), array(3).toLong, array(4).trim, array(5).trim, configuration.data.zoneId)

        case RawAssertionRecord(array) =>
          val assertion: Assertion = {
            // WARN: don't believe IntelliJ here, this import is absolutely mandatory, see
            import io.gatling.shared.model.assertion.AssertionPicklers._
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

    val runMessage = runMessages.headOption.getOrElse(throw new UnsupportedOperationException(s"Files $inputFiles don't contain any valid run record"))
    assert(injectStart != Long.MaxValue, "Undefined run start")
    assert(injectEnd != Long.MinValue, "Undefined run end")
    assert(injectEnd > injectStart, s"Run didn't last")

    RunInfo(injectStart, injectEnd, runMessage.simulationClassName, runMessage.runId, runMessage.runDescription, assertions.toList)
  }

  private def secondPass(runInfo: RunInfo, buckets: Array[Int], bucketFunction: Long => Int, records: Iterator[String]): ResultsHolder = {
    logger.info("Second pass")

    val resultsHolder =
      new ResultsHolder(
        runInfo.injectStart,
        runInfo.injectEnd,
        buckets,
        configuration.reports.indicators.lowerBound,
        configuration.reports.indicators.higherBound
      )

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
}

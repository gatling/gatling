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

import java.{ lang => jl, util => ju }
import java.io.{ BufferedInputStream, DataInputStream, EOFException, File }
import java.nio.ByteBuffer
import java.nio.file.{ Files, Path }
import java.time.ZoneId

import scala.util.Using

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.stats.assertion.Assertion
import io.gatling.commons.util.GatlingVersion
import io.gatling.commons.util.StringHelper._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.message.MessageEvent
import io.gatling.core.stats.writer._

import com.typesafe.scalalogging.StrictLogging
import io.github.metarank.cfor._

private object LogFileParser {
  val LogStep = 100000
}

private abstract class LogFileParser[T](logFile: File) extends AutoCloseable {
  private val is = new DataInputStream(new BufferedInputStream(Files.newInputStream(logFile.toPath)))
  private val skipBuffer = new Array[Byte](1024)
  private val stringCache = new ju.HashMap[Int, String]

  protected def read(): Int = is.read()
  protected def readByte(): Byte = is.readByte()
  protected def readBoolean(): Boolean = is.readBoolean()
  protected def readInt(): Int = is.readInt()
  protected def readByteArray(): Array[Byte] = is.readNBytes(readInt())
  protected def readLong(): Long = is.readLong()
  protected def readString(): String = {
    val length = readInt()
    if (length == 0) {
      ""
    } else {
      val value = is.readNBytes(length)
      val coder = readByte()
      StringInternals.newString(value, coder)
    }
  }
  private def sanitize(s: String): String = s.replaceIf(c => c == '\n' || c == '\r' || c == '\t', ' ')
  protected def readSanitizedString(): String = sanitize(readString())
  protected def readCachedSanitizedString(): String = {
    val cachedIndex = readInt()
    if (cachedIndex >= 0) {
      val string = sanitize(readString())
      stringCache.put(cachedIndex, string)
      string
    } else {
      val cachedString = stringCache.get(-cachedIndex)
      assert(cachedString != null, s"Cached string missing for ${-cachedIndex} index")
      cachedString
    }
  }

  protected def skip(len: Int): Unit = {
    var n = 0
    while (n < len) {
      val count = is.read(skipBuffer, 0, math.min(len - n, skipBuffer.length))
      if (count < 0) {
        throw new EOFException(s"Failed to skip $len bytes")
      }
      n += count
    }
  }
  protected def skipByte(): Unit = skip(jl.Byte.BYTES)
  protected def skipInt(): Unit = skip(jl.Integer.BYTES)
  protected def skipLong(): Unit = skip(jl.Long.BYTES)
  protected def skipString(): Unit = {
    val length = readInt()
    if (length > 0) {
      // value (byte[]) + coder (byte)
      skip(length + 1)
    }
  }
  protected def skipCachedString(): Unit =
    // cachedIndex
    if (readInt() >= 0) {
      skipString()
    }

  override def close(): Unit = is.close()

  def parse(): T
}

private final class FirstPassParser(logFile: File, zoneId: ZoneId) extends LogFileParser[RunInfo](logFile) with StrictLogging {

  private var injectStart = Long.MaxValue
  private var injectEnd = Long.MinValue

  private def updateInjectStart(eventStart: Long): Unit =
    injectStart = math.min(injectStart, eventStart)

  private def updateInjectEnd(eventEnd: Long): Unit =
    injectEnd = math.max(injectEnd, eventEnd)

  private def parseRunRecord(): (RunMessage, Array[String], List[Assertion]) = {
    val gatlingVersion = readString()
    assert(
      gatlingVersion == GatlingVersion.ThisVersion.fullVersion,
      s"The log file $logFile was generated with Gatling $gatlingVersion and can't be parsed with Gatling ${GatlingVersion.ThisVersion.fullVersion}"
    )

    val localRunMessage = RunMessage(
      gatlingVersion = gatlingVersion,
      simulationClassName = readString(),
      simulationId = "", // unused
      start = readLong(),
      runDescription = readString(),
      zoneId = zoneId
    )

    val localScenarios = Array.fill(readInt())(readSanitizedString())

    val localAssertions = List.fill(readInt()) {
      import io.gatling.shared.model.assertion.AssertionPicklers._

      import boopickle.Default._
      val bytes = readByteArray()
      Unpickle.apply[Assertion].fromBytes(ByteBuffer.wrap(bytes))
    }

    (localRunMessage, localScenarios, localAssertions)
  }

  private def parseUserRecord(startTimeStamp: Long): Unit = {
    // scenario
    skipInt()
    val event = if (readBoolean()) MessageEvent.Start else MessageEvent.End
    val timestamp = readInt() + startTimeStamp

    if (event == MessageEvent.Start) {
      updateInjectStart(timestamp)
    }
    updateInjectEnd(timestamp)
  }

  private def parseRequestRecord(startTimeStamp: Long): Unit = {
    // group
    val groupsSize = readInt()
    cfor(0 until groupsSize)(_ => skipCachedString())
    // name
    skipCachedString()
    val startTimestamp = readInt() + startTimeStamp
    val endTimestamp = readInt() + startTimeStamp
    // status
    skipByte()
    // message
    skipCachedString()

    updateInjectStart(startTimestamp)
    updateInjectEnd(endTimestamp)
  }

  private def parseGroupRecord(startTimeStamp: Long): Unit = {
    // group
    val groupsSize = readInt()
    cfor(0 until groupsSize)(_ => skipCachedString())
    val startTimestamp = readInt() + startTimeStamp
    val endTimestamp = readInt() + startTimeStamp
    // cumulatedResponseTime
    skipInt()
    // status
    skipByte()

    updateInjectStart(startTimestamp)
    updateInjectEnd(endTimestamp)
  }

  private def parseErrorRecord(): Unit = {
    // message
    skipCachedString()
    // timestamp
    skipInt()
  }

  override def parse(): RunInfo = {
    logger.info("First pass")
    val (runMessage, scenarios, assertions) = readByte() match {
      case RecordHeader.Run.value => parseRunRecord()
      case _                      => throw new UnsupportedOperationException(s"The log file $logFile is malformed and doesn't start with a proper record")
    }

    var count = 1
    var continue = true
    while (continue) {
      count += 1
      if (count % LogFileParser.LogStep == 0) logger.info(s"First pass, read $count records")
      val headerValue = read().toByte
      try {
        headerValue match {
          case RecordHeader.User.value    => parseUserRecord(runMessage.start)
          case RecordHeader.Request.value => parseRequestRecord(runMessage.start)
          case RecordHeader.Group.value   => parseGroupRecord(runMessage.start)
          case RecordHeader.Error.value   => parseErrorRecord()
          case -1                         => continue = false
          case _                          => throw new UnsupportedOperationException(s"Unsupported header $headerValue for record $count")
        }
      } catch {
        case e: EOFException =>
          logger.error(s"Log file is truncated after record $count, can only generate partial results.", e)
          continue = false
      }
    }

    logger.info(s"First pass done: read $count records")
    assert(injectStart != Long.MaxValue, "Undefined run start")
    assert(injectEnd != Long.MinValue, "Undefined run end")
    assert(injectEnd > injectStart, "Run didn't last")
    new RunInfo(injectStart, injectEnd, runMessage.simulationClassName, runMessage.runDescription, runMessage.start, scenarios, assertions)
  }
}

private final class SecondPassParser(logFile: File, runInfo: RunInfo, step: Double, lowerBound: Int, higherBound: Int)
    extends LogFileParser[ResultsHolder](logFile)
    with StrictLogging {

  private val buckets = StatsHelper.buckets(0, runInfo.injectEnd - runInfo.injectStart, step)
  private val bucketFunction = StatsHelper.timeToBucketNumber(runInfo.injectStart, step, buckets.length)
  private val resultsHolder =
    new ResultsHolder(
      runInfo.injectStart,
      runInfo.injectEnd,
      buckets,
      lowerBound,
      higherBound
    )

  private def skipRunRecord(): Unit = {
    // header
    skipByte()
    // gatlingVersion
    skipString()
    // simulationClassName
    skipString()
    // start
    skipLong()
    // runDescription
    skipString()
    // scenarios
    val scenariosSize = readInt()
    cfor(0 until scenariosSize)(_ => skipString())
    // assertions
    val assertionsSize = readInt()
    cfor(0 until assertionsSize)(_ => skip(readInt()))
  }

  private def parseUserRecord(): UserRecord =
    UserRecord(
      scenario = runInfo.scenarios(readInt()),
      event = if (readBoolean()) MessageEvent.Start else MessageEvent.End,
      timestamp = readInt() + runInfo.runStart
    )

  private def parseRequestRecord(): RequestRecord = {
    val groupsSize = readInt()
    val group = Option.when(groupsSize > 0)(Group(List.fill(groupsSize)(readCachedSanitizedString())))
    val name = readCachedSanitizedString()
    val startTimestamp = readInt() + runInfo.runStart
    val endTimestamp = readInt() + runInfo.runStart
    val status = if (readBoolean()) OK else KO
    val errorMessage = readCachedSanitizedString().trimToOption

    if (endTimestamp != Long.MinValue) {
      // regular request
      RequestRecord(
        group,
        name,
        status,
        startTimestamp,
        bucketFunction(startTimestamp),
        bucketFunction(endTimestamp),
        (endTimestamp - startTimestamp).toInt,
        errorMessage,
        incoming = false
      )
    } else {
      // unmatched incoming event
      RequestRecord(group, name, status, startTimestamp, bucketFunction(startTimestamp), bucketFunction(endTimestamp), 0, errorMessage, incoming = true)
    }
  }

  private def parseGroupRecord(): GroupRecord = {
    val groupsSize = readInt()
    val group = Group(List.fill(groupsSize)(readCachedSanitizedString()))
    val startTimestamp = readInt() + runInfo.runStart
    val endTimestamp = readInt() + runInfo.runStart
    val cumulatedResponseTime = readInt()
    val status = if (readBoolean()) OK else KO

    GroupRecord(group, (endTimestamp - startTimestamp).toInt, cumulatedResponseTime, status, startTimestamp, bucketFunction(startTimestamp))
  }

  private def parseErrorRecord(): ErrorRecord = {
    val message = readCachedSanitizedString()
    val timestamp = readInt() + runInfo.runStart
    ErrorRecord(message, timestamp)
  }

  override def parse(): ResultsHolder = {
    logger.info("Second pass")

    skipRunRecord()

    var count = 1
    var continue = true
    while (continue) {
      count += 1
      if (count % LogFileParser.LogStep == 0) logger.info(s"First pass, read $count records")
      val headerValue = read().toByte

      try {
        headerValue match {
          case RecordHeader.User.value    => resultsHolder.addUserRecord(parseUserRecord())
          case RecordHeader.Request.value => resultsHolder.addRequestRecord(parseRequestRecord())
          case RecordHeader.Group.value   => resultsHolder.addGroupRecord(parseGroupRecord())
          case RecordHeader.Error.value   => resultsHolder.addErrorRecord(parseErrorRecord())
          case -1                         => continue = false
          case _                          => throw new UnsupportedOperationException(s"Unsupported header $headerValue for record $count")
        }
      } catch {
        case e: EOFException =>
          logger.error(s"Log file is truncated after record $count, can only generate partial results.", e)
          continue = false
      }
    }

    resultsHolder.flushTrailingConcurrentUsers()

    logger.info(s"Second pass: read $count records")

    resultsHolder
  }
}

private[gatling] object LogFileReader extends StrictLogging {
  private val SecMillisecRatio: Double = 1000.0

  def apply(runUuid: String, resultsDirectory: Path, configuration: GatlingConfiguration): LogFileReader = {
    StringInternals.checkAvailability()
    val logFile = LogFileDataWriter.logFile(resultsDirectory, runUuid, create = false).toFile

    logger.info(s"Collected $logFile from $runUuid")
    require(logFile.exists(), s"Could not locate log file for $runUuid.")

    new LogFileReader(logFile, configuration)
  }
}

private[gatling] final class LogFileReader(logFile: File, configuration: GatlingConfiguration) extends StrictLogging {
  import LogFileReader._

  def read(): LogFileData = {
    val runInfo = Using.resource(new FirstPassParser(logFile, configuration.data.zoneId))(_.parse())

    val step = StatsHelper.step(
      math.floor(runInfo.injectStart / SecMillisecRatio).toInt,
      math.ceil(runInfo.injectEnd / SecMillisecRatio).toInt,
      configuration.reports.maxPlotsPerSeries
    ) * SecMillisecRatio

    val resultsHolder = Using.resource(
      new SecondPassParser(
        logFile,
        runInfo,
        step,
        configuration.reports.indicators.lowerBound,
        configuration.reports.indicators.higherBound
      )
    )(_.parse())

    new LogFileData(runInfo, resultsHolder, step)
  }
}

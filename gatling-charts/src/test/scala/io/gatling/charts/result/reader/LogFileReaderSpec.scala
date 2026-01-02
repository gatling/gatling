/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.charts.result.reader

import java.{ lang => jl }
import java.io.{ BufferedOutputStream, DataOutputStream, File, FileOutputStream }
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{ Files, Paths }

import scala.util.Using

import io.gatling.charts.stats.{ LogFileData, LogFileReader }
import io.gatling.commons.util.GatlingVersion
import io.gatling.core.config.ConfigKeys._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.writer.LogFileDataWriter

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

@SuppressWarnings(Array("org.wartremover.warts.SeqApply"))
class LogFileReaderSpec extends AnyFlatSpecLike with Matchers {
  private val runUuid = "known_stats"
  private val resultsDirectory = {
    // we need a log file whose version matches Gatling's
    // the sample contains a static value while Gatling's is computed by sbt based on git
    // so we need to replace it
    val rawLogFile = Paths.get(Thread.currentThread().getContextClassLoader.getResource(s"$runUuid/${LogFileDataWriter.LogFileName}").toURI)

    val tmpResultsDirectory = Files.createTempDirectory("gatling")
    val runDirectory = tmpResultsDirectory.resolve(runUuid).toFile
    runDirectory.mkdir()
    val logFileWithMatchingLibraryVersion = new File(runDirectory, LogFileDataWriter.LogFileName)
    logFileWithMatchingLibraryVersion.deleteOnExit()

    Using.resource(new DataOutputStream(new BufferedOutputStream(new FileOutputStream(logFileWithMatchingLibraryVersion)))) { os =>
      val sampleBytes = Files.readAllBytes(rawLogFile)
      // run record header
      os.write(sampleBytes, 0, jl.Byte.BYTES)
      val gatlingFullVersionBytes = GatlingVersion.ThisVersion.fullVersion.getBytes(UTF_8)
      // Gatling version length
      os.writeInt(gatlingFullVersionBytes.length)
      os.write(gatlingFullVersionBytes)
      // rest of the original file, after skipping the original Gatling version in the file
      val offset = jl.Byte.BYTES + jl.Integer.BYTES + "3.12.1".getBytes(UTF_8).length
      os.write(sampleBytes, offset, sampleBytes.length - offset)
    }

    tmpResultsDirectory
  }

  private def logFileData(props: (String, _ <: Any)*): LogFileData = {
    val configuration = GatlingConfiguration.loadForTest(props: _*)
    LogFileReader(runUuid, resultsDirectory, configuration).read()
  }

  "When reading a single log file with known statistics, FileDataReader" should "return expected minResponseTime for correct request data" in {
    logFileData().requestGeneralStats(None, None, None).map(_.min).getOrElse(throw new IllegalStateException) shouldBe 87
  }

  it should "return expected maxResponseTime for correct request data" in {
    logFileData().requestGeneralStats(None, None, None).map(_.max).getOrElse(throw new IllegalStateException) shouldBe 368
  }

  it should "return expected responseTimeStandardDeviation for correct request data" in {
    val computedValue = logFileData().requestGeneralStats(None, None, None).map(_.stdDev).getOrElse(throw new IllegalStateException)
    val expectedValue = 2138
    val error = (computedValue.toDouble - expectedValue) / expectedValue

    error shouldBe <=(0.06)
  }

  it should "return expected responseTimePercentile for the p0 and p70" in {
    val fileData = logFileData(
      charting.indicators.Percentile1 -> 0,
      charting.indicators.Percentile2 -> 70
    )
    fileData.requestGeneralStats(None, None, None).map(_.percentile(0.0)).getOrElse(throw new IllegalStateException) shouldBe 87
    fileData.requestGeneralStats(None, None, None).map(_.percentile(70.0)).getOrElse(throw new IllegalStateException) shouldBe 113
  }

  it should "return expected result for the p99 and p100" in {
    val fileData = logFileData(
      charting.indicators.Percentile1 -> 99,
      charting.indicators.Percentile2 -> 100
    )
    fileData.requestGeneralStats(None, None, None).map(_.percentile(99.0)).getOrElse(throw new IllegalStateException) shouldBe 368
    fileData.requestGeneralStats(None, None, None).map(_.percentile(100.0)).getOrElse(throw new IllegalStateException) shouldBe 368
  }

  it should "indicate that all the request have their response time in between 0 and 100000" in {
    val fileData = logFileData(
      charting.indicators.LowerBound -> 0,
      charting.indicators.HigherBound -> 100000
    )
    val ranges = fileData.numberOfRequestInResponseTimeRanges(None, None)
    ranges.lowCount shouldBe 0
    ranges.middleCount shouldBe 104
    ranges.highCount shouldBe 0
    ranges.koCount shouldBe 1
  }

  it should "indicate that 1 request had a response time below 2500ms" in {
    val fileData = logFileData(
      charting.indicators.LowerBound -> 2500,
      charting.indicators.HigherBound -> 5000
    )
    fileData.numberOfRequestInResponseTimeRanges(None, None).lowCount shouldBe 104
  }

  it should "indicate that 5 request had a response time in between 2500ms and 5000ms" in {
    val fileData = logFileData(
      charting.indicators.LowerBound -> 2500,
      charting.indicators.HigherBound -> 5000
    )
    fileData.numberOfRequestInResponseTimeRanges(None, None).middleCount shouldBe 0
  }

  it should "indicate that 2 request had a response time above 5000ms" in {
    val fileData = logFileData(
      charting.indicators.LowerBound -> 2500,
      charting.indicators.HigherBound -> 5000
    )
    fileData.numberOfRequestInResponseTimeRanges(None, None).highCount shouldBe 0
  }
}

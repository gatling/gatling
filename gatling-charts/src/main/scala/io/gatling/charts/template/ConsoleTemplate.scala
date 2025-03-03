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

package io.gatling.charts.template

import java.{ lang => jl }

import io.gatling.charts.component.{ RequestStatistics, Stats }
import io.gatling.commons.util.StringHelper._
import io.gatling.core.stats.ErrorStats
import io.gatling.core.stats.writer.ConsoleStatsFormat._
import io.gatling.core.stats.writer.ConsoleSummary._

private[charts] object ConsoleTemplate {
  private[template] def writeRequestCounters[T: Numeric](statistics: Stats[T]): String = {
    import statistics._
    s"$Header${name.rightPad(ConsoleWidth - HeaderLength - 3 * (NumberLength + 3))} | ${formatNumber(total)} | ${formatNumber(success)} | ${formatNumber(failure)}"
  }

  private[template] def writeRange(textLabel: String, count: Int, percentage: Double): String =
    s"$Header${textLabel.rightPad(ConsoleWidth - HeaderLength - NumberLength - PercentageLength - 2)} ${formatNumber(count)} ${formatPercentage(percentage)}"

  private[template] def writeErrorsBlock(sb: jl.StringBuilder, errors: Seq[ErrorStats]): jl.StringBuilder = {
    if (errors.nonEmpty) {
      sb.append(formatSubTitle("Errors")).append(Eol)
      errors.foreach(writeError(sb, _).append(Eol))
    }
    sb
  }
}

private[charts] class ConsoleTemplate(requestStatistics: RequestStatistics, errors: Seq[ErrorStats]) {

  import ConsoleTemplate._

  def getOutput: String = {
    import requestStatistics._

    val sb = new jl.StringBuilder()
      .append(s"""
                 |$NewBlock
                 |${formatSubTitleWithStatuses("Global Information")}
                 |${writeRequestCounters(numberOfRequestsStatistics)}
                 |${writeRequestCounters(minResponseTimeStatistics)}
                 |${writeRequestCounters(maxResponseTimeStatistics)}
                 |${writeRequestCounters(meanResponseTimeStatistics)}
                 |${writeRequestCounters(stdDeviationStatistics)}
                 |${writeRequestCounters(percentiles1)}
                 |${writeRequestCounters(percentiles2)}
                 |${writeRequestCounters(percentiles3)}
                 |${writeRequestCounters(percentiles4)}
                 |${writeRequestCounters(meanNumberOfRequestsPerSecondStatistics)}
                 |${"---- Response Time Distribution ".rightPad(ConsoleWidth, "-")}
                 |${writeRange(s"OK: t < ${ranges.lowerBound} ms", ranges.lowCount, ranges.lowPercentage)}
                 |${writeRange(s"OK: ${ranges.lowerBound} ms <= t < ${ranges.higherBound} ms", ranges.middleCount, ranges.middlePercentage)}
                 |${writeRange(s"OK: t >= ${ranges.higherBound} ms", ranges.highCount, ranges.highPercentage)}
                 |${writeRange("KO", ranges.koCount, ranges.koPercentage)}
                 |""".stripMargin)

    writeErrorsBlock(sb, errors)
    sb.append(NewBlock).append(Eol).toString
  }
}

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

package io.gatling.charts.template

import java.{ lang => jl }

import io.gatling.charts.component.{ GroupedCount, RequestStatistics, Statistics }
import io.gatling.charts.component.Statistics.printable
import io.gatling.commons.shared.unstable.model.stats.ErrorStats
import io.gatling.commons.util.StringHelper._
import io.gatling.core.stats.writer.ConsoleErrorsWriter
import io.gatling.core.stats.writer.ConsoleSummary._

private[charts] object ConsoleTemplate {

  private[template] def writeRequestCounters[T: Numeric](sb: jl.StringBuilder, statistics: Statistics[T]): jl.StringBuilder = {
    import statistics._
    sb.append("> ")
      .append(name.rightPad(OutputLength - 32))
      .append(' ')
      .append(printable(total).leftPad(7))
      .append(" (OK=")
      .append(printable(success).rightPad(6))
      .append(" KO=")
      .append(printable(failure).rightPad(6))
      .append(')')
  }

  private[template] def writeGroupedCounters(sb: jl.StringBuilder, groupedCount: GroupedCount): jl.StringBuilder = {
    import groupedCount._
    sb.append("> ")
      .append(name.rightPad(OutputLength - 32))
      .append(' ')
      .append(count.toString.leftPad(7))
      .append(" (")
      .append(percentage.toString.leftPad(3))
      .append("%)")
  }

  private[template] def writeErrorsAndEndBlock(sb: jl.StringBuilder, errors: Seq[ErrorStats]): jl.StringBuilder = {
    if (errors.nonEmpty) {
      writeSubTitle(sb, "Errors").append(Eol)
      errors.foreach(ConsoleErrorsWriter.writeError(sb, _).append(Eol))
    }
    sb.append(NewBlock)
  }

  def println(requestStatistics: RequestStatistics, errors: Seq[ErrorStats]): String = {
    import requestStatistics._

    val sb = new jl.StringBuilder()
      .append(Eol)
      .append(NewBlock)
      .append(Eol)

    writeSubTitle(sb, "Global Information").append(Eol)
    writeRequestCounters(sb, numberOfRequestsStatistics).append(Eol)
    writeRequestCounters(sb, minResponseTimeStatistics).append(Eol)
    writeRequestCounters(sb, maxResponseTimeStatistics).append(Eol)
    writeRequestCounters(sb, meanStatistics).append(Eol)
    writeRequestCounters(sb, stdDeviationStatistics).append(Eol)
    writeRequestCounters(sb, percentiles1).append(Eol)
    writeRequestCounters(sb, percentiles2).append(Eol)
    writeRequestCounters(sb, percentiles3).append(Eol)
    writeRequestCounters(sb, percentiles4).append(Eol)
    writeRequestCounters(sb, meanNumberOfRequestsPerSecondStatistics).append(Eol)
    writeSubTitle(sb, "Response Time Distribution").append(Eol)
    groupedCounts.foreach(writeGroupedCounters(sb, _).append(Eol))
    writeErrorsAndEndBlock(sb, errors).append(Eol).toString
  }
}

/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import java.lang.{ StringBuilder => JStringBuilder }

import io.gatling.commons.stats.ErrorStats
import io.gatling.commons.util.StringHelper._
import io.gatling.charts.component.Statistics
import io.gatling.charts.component.Statistics.printable
import io.gatling.charts.component.{ GroupedCount, RequestStatistics }
import io.gatling.core.stats.writer.ConsoleErrorsWriter
import io.gatling.core.stats.writer.ConsoleSummary._

private[charts] object ConsoleTemplate {

  private[template] def writeRequestCounters[T: Numeric](sb: JStringBuilder, statistics: Statistics[T]): JStringBuilder = {
    import statistics._
    sb.append("> ").append(name.rightPad(OutputLength - 32)).append(' ').append(printable(total).leftPad(7)).append(" (OK=").append(printable(success).rightPad(6)).append(" KO=").append(printable(failure).rightPad(6)).append(')')
  }

  private[template] def writeGroupedCounters(sb: JStringBuilder, groupedCount: GroupedCount): JStringBuilder = {
    import groupedCount._
    sb.append("> ").append(name.rightPad(OutputLength - 32)).append(' ').append(count.toString.leftPad(7)).append(" (").append(percentage.toString.leftPad(3)).append("%)")
  }

  private[template] def writeErrorsAndEndBlock(sb: JStringBuilder, errors: Seq[ErrorStats]): JStringBuilder = {
    if (errors.nonEmpty) {
      writeSubTitle(sb, "Errors").append(Eol)
      errors.foreach(ConsoleErrorsWriter.writeError(sb, _).append(Eol))
    }
    sb.append(NewBlock)
  }

  def println(requestStatistics: RequestStatistics, errors: Seq[ErrorStats]): String = {
    import requestStatistics._

    val sb = new JStringBuilder().append(Eol)
      .append(NewBlock).append(Eol)

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

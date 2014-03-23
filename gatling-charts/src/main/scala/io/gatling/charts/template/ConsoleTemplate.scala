/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.charts.template

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.charts.component.{ GroupedCount, RequestStatistics, Statistics }
import io.gatling.charts.component.Statistics.printable
import io.gatling.core.result.writer.ConsoleSummary.{ newBlock, outputLength, writeSubTitle }
import io.gatling.core.util.StringHelper.{ eol, RichString }

object ConsoleTemplate {

  def writeRequestCounters[T: Numeric](statistics: Statistics[T]): Fastring = {
    import statistics._
    fast"> ${name.rightPad(outputLength - 32)} ${printable(total).leftPad(7)} (OK=${printable(success).rightPad(6)} KO=${printable(failure).rightPad(6)})"
  }
  def writeGroupedCounters(groupedCount: GroupedCount): Fastring = {
    import groupedCount._
    fast"> ${name.rightPad(outputLength - 32)} ${count.toString.leftPad(7)} (${percentage.toString.leftPad(3)}%)"
  }

  def apply(requestStatistics: RequestStatistics): String = {
    import requestStatistics._
    fast"""
$newBlock
${writeSubTitle("Global Information")}
${writeRequestCounters(numberOfRequestsStatistics)}
${writeRequestCounters(minResponseTimeStatistics)}
${writeRequestCounters(maxResponseTimeStatistics)}
${writeRequestCounters(meanStatistics)}
${writeRequestCounters(stdDeviationStatistics)}
${writeRequestCounters(percentiles1)}
${writeRequestCounters(percentiles2)}
${writeRequestCounters(meanNumberOfRequestsPerSecondStatistics)}
${writeSubTitle("Response Time Distribution")}
${groupedCounts.map(writeGroupedCounters).mkFastring(eol)}
$newBlock
""".toString
  }
}

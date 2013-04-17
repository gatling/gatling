/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
import io.gatling.core.result.writer.ConsoleSummary.{ newBlock, outputLength, writeSubTitle }
import io.gatling.core.util.StringHelper.RichString

class ConsoleTemplate(requestStatistics: RequestStatistics) {

	def writeRequestCounters(actionName: String, statistics: Statistics): Fastring = {
		import statistics._
		fast"> ${actionName.rightPad(outputLength - 32)} TO=${printableTotal.rightPad(6)} OK=${printableSuccess.rightPad(6)} KO=${printableFailure.rightPad(6)}"
	}
	def writeLatencyCounters(groupedCount: GroupedCount): Fastring = {
		import groupedCount._
		fast"> ${name.rightPad(outputLength - 32)} COUNT=${count.toString.rightPad(6)} PERCENTAGE=${percentage.toString.rightPad(6)}"
	}

	def getOutput: String = {
		import requestStatistics._
		fast"""
$newBlock
${writeSubTitle("Global Information")}
${writeRequestCounters("Number of Requests", numberOfRequestsStatistics)}
${writeRequestCounters("Min Response Time", minResponseTimeStatistics)}
${writeRequestCounters("Max Response Time", maxResponseTimeStatistics)}
${writeRequestCounters("Mean Response Time", meanStatistics)}
${writeRequestCounters("Standard Deviation Time", stdDeviationStatistics)}
${writeRequestCounters("Percentile 1", percentiles1)}
${writeRequestCounters("Percentile 2", percentiles2)}
${writeRequestCounters("Mean Number Of Requests Per Second", meanNumberOfRequestsPerSecondStatistics)}
${writeSubTitle("Request Latency Distribution")}
${writeLatencyCounters(groupedCounts(0))}
${writeLatencyCounters(groupedCounts(1))}
${writeLatencyCounters(groupedCounts(2))}
$newBlock
""".toString
	}
}

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
import org.joda.time.DateTime
import scala.math.{ ceil, floor, max }
import org.joda.time.format.DateTimeFormat
import io.gatling.core.util.PaddableStringBuilder
import io.gatling.core.util.StringHelper.eol
import io.gatling.charts.component.RequestStatistics
import io.gatling.charts.component.RequestStatistics

class ConsoleTemplate(stats: RequestStatistics) {
	val outputLength = 80
	
	def times(s: String, n: Int) = s"${s * n}"
	def rightPadded(s: String, size: Int) = s"""$s${" " * (size - s.length)}"""
	def newBlock :Fastring = { fast"""${times("=",outputLength)}"""}
	
	def appendSubTitle(title: String) :Fastring =  fast"""---- $title ${times("-", max(outputLength - title.length - 6, 0))}""" 
	def appendRequestCounters(actionName: String, total: String, ok: String, ko: String):Fastring =  fast"""> ${rightPadded(actionName, outputLength - 32)} TO=${rightPadded(total, 6)} OK=${rightPadded(ok, 6)} KO=${rightPadded(ko, 6)}"""	
	def appendLatencyCounters(name: String, count: String, percentage: String):Fastring = fast"""> ${rightPadded(name, outputLength - 32)} COUNT=${rightPadded(count, 6)} PERCENTAGE=${rightPadded(percentage, 6)}"""
   
	def getOutput: String = {
	  import stats._
fast"""
${newBlock}
${appendSubTitle("Global Information")}
${appendRequestCounters("Number of Requests", numberOfRequestsStatistics.printableTotal, numberOfRequestsStatistics.printableSuccess, numberOfRequestsStatistics.printableFailure)}
${appendRequestCounters("Min Response Time", minResponseTimeStatistics.printableTotal, minResponseTimeStatistics.printableSuccess, minResponseTimeStatistics.printableFailure)}
${appendRequestCounters("Max Response Time", maxResponseTimeStatistics.printableTotal, maxResponseTimeStatistics.printableSuccess, maxResponseTimeStatistics.printableFailure)}
${appendRequestCounters("Mean Response Time", meanStatistics.printableTotal, meanStatistics.printableSuccess, meanStatistics.printableFailure)}
${appendRequestCounters("Standard Deviation Time", stdDeviationStatistics.printableTotal, stdDeviationStatistics.printableSuccess, stdDeviationStatistics.printableFailure)}
${appendRequestCounters("Percentile 1", percentiles1.printableTotal, percentiles1.printableSuccess, percentiles1.printableFailure)}
${appendRequestCounters("Percentile 2", percentiles2.printableTotal, percentiles2.printableSuccess, percentiles2.printableFailure)}
${appendRequestCounters("Mean Number Of Requests Per Second", meanNumberOfRequestsPerSecondStatistics.printableTotal, meanNumberOfRequestsPerSecondStatistics.printableSuccess, meanNumberOfRequestsPerSecondStatistics.printableFailure)}
${appendSubTitle("Request Latency Distribution")}
${appendLatencyCounters(groupedCounts(0).name, groupedCounts(0).count.toString , groupedCounts(0).percentage.toString)}
${appendLatencyCounters(groupedCounts(1).name, groupedCounts(1).count.toString , groupedCounts(1).percentage.toString)}
${appendLatencyCounters(groupedCounts(2).name, groupedCounts(2).count.toString , groupedCounts(2).percentage.toString)}
${newBlock}
""".toString
	} 
}

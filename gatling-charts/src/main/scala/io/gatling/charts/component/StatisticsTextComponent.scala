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
package io.gatling.charts.component

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.charts.config.ChartsFiles.GLOBAL_PAGE_NAME
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.reader.DataReader.NO_PLOT_MAGIC_VALUE
import io.gatling.core.util.NumberHelper.formatNumberWithSuffix
import io.gatling.core.util.StringHelper.emptyFastring

object Statistics {
	def printable[T: Numeric](value: T) =
		value match {
			case NO_PLOT_MAGIC_VALUE => "-"
			case (_: Int) | (_: Long) => value.toString
			case _ =>
				implicitly[Numeric[T]].toDouble(value) match {
					case d if d >= 1000.0d => d.round.toString
					case d if d >= 100.0d => f"$d%.2f"
					case d => f"$d%.2f"
				}
		}
}

case class Statistics[T: Numeric](name: String, total: T, success: T, failure: T) {
	def all = List(total, success, failure)
}

case class GroupedCount(name: String, count: Int, percentage: Int)

case class RequestStatistics(name: String,
	path: String,
	numberOfRequestsStatistics: Statistics[Int],
	minResponseTimeStatistics: Statistics[Int],
	maxResponseTimeStatistics: Statistics[Int],
	meanStatistics: Statistics[Int],
	stdDeviationStatistics: Statistics[Int],
	percentiles1: Statistics[Int],
	percentiles2: Statistics[Int],
	groupedCounts: Seq[GroupedCount],
	meanNumberOfRequestsPerSecondStatistics: Statistics[Double]) {

	def mkFastring = {
		val outputName = List(if (name == GLOBAL_PAGE_NAME) name else path)
		List(
			outputName,
			numberOfRequestsStatistics.all,
			minResponseTimeStatistics.all,
			maxResponseTimeStatistics.all,
			meanStatistics.all,
			stdDeviationStatistics.all,
			percentiles1.all,
			percentiles2.all,
			groupedCounts.flatMap(groupedCount => List(groupedCount.name, groupedCount.count, groupedCount.percentage)),
			meanNumberOfRequestsPerSecondStatistics.all).flatten.mkFastring(configuration.charting.statsTsvSeparator)
	}
}

class StatisticsTextComponent extends Component {

	def html = fast"""
                        <div class="infos">
                            <div class="infos-in">
	                        <div class="infos-title">STATISTICS</div>
                                <div class="repli"></div>                               
                                <div class="info">
                                    <h2 class="first">Executions</h2>
                                    <table>
                                        <thead>
                                            <tr><th></th><th>Total</th><th>OK</th><th>KO</th></tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td class="title"></td>
                                                <td id="numberOfRequests" class="total"></td>
                                                <td id="numberOfRequestsOK" class="ok"></td>
                                                <td id="numberOfRequestsKO" class="ko"></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                    <h2 class="second">Response Time (ms)</h2>
                                    <table>
                                        <thead>
                                            <tr>
                                                <th></th>
                                                <th>Total</th>
                                                <th>OK</th>
                                                <th>KO</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td class="title">Min</td>
                                                <td id="minResponseTime" class="total"></td>
                                                <td id="minResponseTimeOK" class="ok"></td>
                                                <td id="minResponseTimeKO" class="ko"></td>
                                            </tr>
                                            <tr>
                                                <td class="title">Max</td>
                                                <td id="maxResponseTime" class="total"></td>
                                                <td id="maxResponseTimeOK" class="ok"></td>
                                                <td id="maxResponseTimeKO" class="ko"></td>
                                            </tr>
                                            <tr>
                                                <td class="title">Mean</td>
                                                <td id="meanResponseTime" class="total"></td>
                                                <td id="meanResponseTimeOK" class="ok"></td>
                                                <td id="meanResponseTimeKO" class="ko"></td>
                                            </tr>
                                            <tr>
                                                <td class="title">Std Deviation</td>
                                                <td id="standardDeviation" class="total"></td>
                                                <td id="standardDeviationOK" class="ok"></td>
                                                <td id="standardDeviationKO" class="ko"></td>
                                            </tr>
                                            <tr>
                                                <td class="title">${formatNumberWithSuffix(configuration.charting.indicators.percentile1)} percentile</td>
                                                <td id="percentiles1" class="total"></td>
                                                <td id="percentiles1OK" class="ok"></td>
                                                <td id="percentiles1KO" class="ko"></td>
                                            </tr>
                                            <tr>
                                                <td class="title">${formatNumberWithSuffix(configuration.charting.indicators.percentile2)} percentile</td>
                                                <td id="percentiles2" class="total"></td>
                                                <td id="percentiles2OK" class="ok"></td>
                                                <td id="percentiles2KO" class="ko"></td>
                                            </tr>
                                            <tr>
                                                <td class="title">Mean req/s</td>
                                                <td id="meanNumberOfRequestsPerSecond" class="total"></td>
                                                <td id="meanNumberOfRequestsPerSecondOK" class="ok"></td>
                                                <td id="meanNumberOfRequestsPerSecondKO" class="ko"></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
"""

	val js = emptyFastring

	val jsFiles: Seq[String] = Seq.empty
}
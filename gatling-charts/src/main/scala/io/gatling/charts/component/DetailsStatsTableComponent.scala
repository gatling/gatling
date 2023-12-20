/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.charts.component

import io.gatling.charts.stats.{ GeneralStats, Ranges }
import io.gatling.core.config.IndicatorsConfiguration
import io.gatling.shared.util.NumberHelper._

private[charts] object Stats {
  def printable[T: Numeric](value: T): String =
    value match {
      case GeneralStats.NoPlotMagicValue => "-"
      case _: Int | _: Long              => value.toString
      case _                             => implicitly[Numeric[T]].toDouble(value).toPrintableString
    }
}

private[charts] final class Stats[T: Numeric](val name: String, val total: T, val success: T, val failure: T)

private[charts] final class RequestStatistics(
    val name: String,
    val path: String,
    val numberOfRequestsStatistics: Stats[Long],
    val minResponseTimeStatistics: Stats[Int],
    val maxResponseTimeStatistics: Stats[Int],
    val meanResponseTimeStatistics: Stats[Int],
    val stdDeviationStatistics: Stats[Int],
    val percentiles1: Stats[Int],
    val percentiles2: Stats[Int],
    val percentiles3: Stats[Int],
    val percentiles4: Stats[Int],
    val ranges: Ranges,
    val meanNumberOfRequestsPerSecondStatistics: Stats[Double]
)

private[charts] final class DetailsStatsTableComponent(configuration: IndicatorsConfiguration) extends Component {
  override def html: String = s"""
                        <div class="infos">
                            <div class="infos-in">
	                        <div class="infos-title">Stats</div>
                                <div class="info">
                                    <h2 class="first">Executions</h2>
                                    <table>
                                        <thead>
                                            <tr><th></th><th>Total</th><th>OK</th><th>KO</th></tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td class="title">Total count</td>
                                                <td id="numberOfRequests" class="total"></td>
                                                <td id="numberOfRequestsOK" class="ok"></td>
                                                <td id="numberOfRequestsKO" class="ko"></td>
                                            </tr>
                                            <tr>
                                                <td class="title">Mean count/s</abbr></td>
                                                <td id="meanNumberOfRequestsPerSecond" class="total"></td>
                                                <td id="meanNumberOfRequestsPerSecondOK" class="ok"></td>
                                                <td id="meanNumberOfRequestsPerSecondKO" class="ko"></td>
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
                                                <td class="title">${configuration.percentile1.toRank} percentile</td>
                                                <td id="percentiles1" class="total"></td>
                                                <td id="percentiles1OK" class="ok"></td>
                                                <td id="percentiles1KO" class="ko"></td>
                                            </tr>
                                            <tr>
                                                <td class="title">${configuration.percentile2.toRank} percentile</td>
                                                <td id="percentiles2" class="total"></td>
                                                <td id="percentiles2OK" class="ok"></td>
                                                <td id="percentiles2KO" class="ko"></td>
                                            </tr>
                                            <tr>
                                                <td class="title">${configuration.percentile3.toRank} percentile</td>
                                                <td id="percentiles3" class="total"></td>
                                                <td id="percentiles3OK" class="ok"></td>
                                                <td id="percentiles3KO" class="ko"></td>
                                            </tr>
                                            <tr>
                                                <td class="title">${configuration.percentile4.toRank} percentile</td>
                                                <td id="percentiles4" class="total"></td>
                                                <td id="percentiles4OK" class="ok"></td>
                                                <td id="percentiles4KO" class="ko"></td>
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
                                                <td class="title">Standard Deviation</td>
                                                <td id="standardDeviation" class="total"></td>
                                                <td id="standardDeviationOK" class="ok"></td>
                                                <td id="standardDeviationKO" class="ko"></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
"""

  val js = ""

  val jsFiles: Seq[String] = Nil
}

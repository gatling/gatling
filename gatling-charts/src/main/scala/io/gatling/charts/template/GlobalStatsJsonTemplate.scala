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

import io.gatling.charts.component.RequestStatistics
import io.gatling.charts.component.Statistics.printable
import io.gatling.charts.util.JsHelper._
import io.gatling.commons.shared.unstable.model.stats.GeneralStats

private[charts] class GlobalStatsJsonTemplate(stats: RequestStatistics, raw: Boolean) {

  private def group(i: Int) =
    s""""group${i + 1}": {
       |    "name": "${stats.groupedCounts(i).name}",
       |    "count": ${stats.groupedCounts(i).count},
       |    "percentage": ${stats.groupedCounts(i).percentage}
       |}""".stripMargin

  def getOutput: String = {

    def style[T: Numeric](value: T) =
      if (raw) {
        // raw mode is used for JSON extract, non-raw for displaying in the reports
        if (value == GeneralStats.NoPlotMagicValue) "0"
        else value.toString
      } else
        s""""${printable(value)}""""

    s"""{
    "name": "${escapeJsIllegalChars(stats.name)}",
    "numberOfRequests": {
        "total": ${style(stats.numberOfRequestsStatistics.total)},
        "ok": ${style(stats.numberOfRequestsStatistics.success)},
        "ko": ${style(stats.numberOfRequestsStatistics.failure)}
    },
    "minResponseTime": {
        "total": ${style(stats.minResponseTimeStatistics.total)},
        "ok": ${style(stats.minResponseTimeStatistics.success)},
        "ko": ${style(stats.minResponseTimeStatistics.failure)}
    },
    "maxResponseTime": {
        "total": ${style(stats.maxResponseTimeStatistics.total)},
        "ok": ${style(stats.maxResponseTimeStatistics.success)},
        "ko": ${style(stats.maxResponseTimeStatistics.failure)}
    },
    "meanResponseTime": {
        "total": ${style(stats.meanStatistics.total)},
        "ok": ${style(stats.meanStatistics.success)},
        "ko": ${style(stats.meanStatistics.failure)}
    },
    "standardDeviation": {
        "total": ${style(stats.stdDeviationStatistics.total)},
        "ok": ${style(stats.stdDeviationStatistics.success)},
        "ko": ${style(stats.stdDeviationStatistics.failure)}
    },
    "percentiles1": {
        "total": ${style(stats.percentiles1.total)},
        "ok": ${style(stats.percentiles1.success)},
        "ko": ${style(stats.percentiles1.failure)}
    },
    "percentiles2": {
        "total": ${style(stats.percentiles2.total)},
        "ok": ${style(stats.percentiles2.success)},
        "ko": ${style(stats.percentiles2.failure)}
    },
    "percentiles3": {
        "total": ${style(stats.percentiles3.total)},
        "ok": ${style(stats.percentiles3.success)},
        "ko": ${style(stats.percentiles3.failure)}
    },
    "percentiles4": {
        "total": ${style(stats.percentiles4.total)},
        "ok": ${style(stats.percentiles4.success)},
        "ko": ${style(stats.percentiles4.failure)}
    },
    ${group(0)},
    ${group(1)},
    ${group(2)},
    ${group(3)},
    "meanNumberOfRequestsPerSecond": {
        "total": ${style(stats.meanNumberOfRequestsPerSecondStatistics.total)},
        "ok": ${style(stats.meanNumberOfRequestsPerSecondStatistics.success)},
        "ko": ${style(stats.meanNumberOfRequestsPerSecondStatistics.failure)}
    }
}"""
  }
}

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

import java.text.{ DecimalFormat, DecimalFormatSymbols }
import java.util.Locale

import io.gatling.charts.component.RequestStatistics
import io.gatling.charts.util.JsHelper._
import io.gatling.core.stats.NoPlotMagicValue

private object GlobalStatsJsonTemplate {

  private val Formatter = new DecimalFormat("###.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH))

  def formatNumber[T: Numeric](value: T): String =
    Formatter.format(implicitly[Numeric[T]].toDouble(value))
}

@SuppressWarnings(Array("org.wartremover.warts.SeqApply"))
private[charts] final class GlobalStatsJsonTemplate(stats: RequestStatistics, raw: Boolean) {

  private def group(index: Int, label: String, count: Int, percentage: Double): String =
    group(index, label, label, count, percentage)

  private def group(index: Int, textLabel: String, htmlLabel: String, count: Int, percentage: Double): String =
    s""""group${index + 1}": {
       |    "name": "$textLabel",
       |    "htmlName": "$htmlLabel",
       |    "count": $count,
       |    "percentage": $percentage
       |}""".stripMargin

  def getOutput: String = {
    import stats._
    def style[T: Numeric](value: T) =
      if (raw) {
        // raw mode is used for JSON extract, non-raw for displaying in the reports
        if (implicitly[Numeric[T]].toInt(value) == NoPlotMagicValue) "0"
        else value.toString
      } else {
        val string = value match {
          case NoPlotMagicValue => "-"
          case _                => GlobalStatsJsonTemplate.formatNumber(value)
        }
        s""""$string""""
      }

    s"""{
    "name": "${escapeJsIllegalChars(name)}",
    "numberOfRequests": {
        "total": ${style(numberOfRequestsStatistics.total)},
        "ok": ${style(numberOfRequestsStatistics.success)},
        "ko": ${style(numberOfRequestsStatistics.failure)}
    },
    "minResponseTime": {
        "total": ${style(minResponseTimeStatistics.total)},
        "ok": ${style(minResponseTimeStatistics.success)},
        "ko": ${style(minResponseTimeStatistics.failure)}
    },
    "maxResponseTime": {
        "total": ${style(maxResponseTimeStatistics.total)},
        "ok": ${style(maxResponseTimeStatistics.success)},
        "ko": ${style(maxResponseTimeStatistics.failure)}
    },
    "meanResponseTime": {
        "total": ${style(meanResponseTimeStatistics.total)},
        "ok": ${style(meanResponseTimeStatistics.success)},
        "ko": ${style(meanResponseTimeStatistics.failure)}
    },
    "standardDeviation": {
        "total": ${style(stdDeviationStatistics.total)},
        "ok": ${style(stdDeviationStatistics.success)},
        "ko": ${style(stdDeviationStatistics.failure)}
    },
    "percentiles1": {
        "total": ${style(percentiles1.total)},
        "ok": ${style(percentiles1.success)},
        "ko": ${style(percentiles1.failure)}
    },
    "percentiles2": {
        "total": ${style(percentiles2.total)},
        "ok": ${style(percentiles2.success)},
        "ko": ${style(percentiles2.failure)}
    },
    "percentiles3": {
        "total": ${style(percentiles3.total)},
        "ok": ${style(percentiles3.success)},
        "ko": ${style(percentiles3.failure)}
    },
    "percentiles4": {
        "total": ${style(percentiles4.total)},
        "ok": ${style(percentiles4.success)},
        "ko": ${style(percentiles4.failure)}
    },
    ${group(
        0,
        s"t < ${ranges.lowerBound} ms",
        ranges.lowCount,
        ranges.lowPercentage
      )},
    ${group(
        1,
        s"${ranges.lowerBound} ms <= t < ${ranges.higherBound} ms",
        s"t >= ${ranges.lowerBound} ms <br> t < ${ranges.higherBound} ms",
        ranges.middleCount,
        ranges.middlePercentage
      )},
    ${group(
        2,
        s"t >= ${ranges.higherBound} ms",
        ranges.highCount,
        ranges.highPercentage
      )},
    ${group(
        3,
        "failed",
        ranges.koCount,
        ranges.koPercentage
      )},
    "meanNumberOfRequestsPerSecond": {
        "total": ${style(stats.meanNumberOfRequestsPerSecondStatistics.total)},
        "ok": ${style(stats.meanNumberOfRequestsPerSecondStatistics.success)},
        "ko": ${style(stats.meanNumberOfRequestsPerSecondStatistics.failure)}
    }
}"""
  }
}

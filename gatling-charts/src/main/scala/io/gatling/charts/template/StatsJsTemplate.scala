/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.nio.charset.Charset

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.core.util.StringHelper.RichString
import io.gatling.charts.FileNamingConventions
import io.gatling.charts.component.RequestStatistics
import io.gatling.charts.report.{ GroupContainer, RequestContainer }
import io.gatling.charts.report.Container.{ Group, Request }

private[charts] class StatsJsTemplate(stats: GroupContainer, outputJson: Boolean) {

  private def fieldName(field: String) = if (outputJson) '"' + field + '"' else field

  def getOutput(charset: Charset): Fastring = {

      def renderStatsRequest(request: RequestStatistics): Fastring = {
        val jsonStats = new GlobalStatsJsonTemplate(request, outputJson).getOutput

        fast"""${fieldName("name")}: "${request.name.escapeJsDoubleQuoteString}",
${fieldName("path")}: "${request.path.escapeJsDoubleQuoteString}",
${fieldName("pathFormatted")}: "${request.path.toFileName(charset)}",
${fieldName("stats")}: $jsonStats"""
      }

      def renderStatsGroup(group: GroupContainer): Fastring =
        fast"""${fieldName("type")}: "$Group",
${renderStatsRequest(group.stats)},
${fieldName("contents")}: {
${
          group.contents.values.map {
            case subGroup: GroupContainer => fast""""${subGroup.name.toFileName(charset)}": {
        ${renderStatsGroup(subGroup)}
    }"""
            case request: RequestContainer => fast""""${request.name.toFileName(charset)}": {
        ${fieldName("type")}: "$Request",
        ${renderStatsRequest(request.stats)}
    }"""
          }.mkFastring(",")
        }
}
"""

    if (outputJson)
      fast"""{
  ${renderStatsGroup(stats)}
}"""
    else
      fast"""var stats = {
    ${renderStatsGroup(stats)}
}

function fillStats(stat){
    $$("#numberOfRequests").append(stat.numberOfRequests.total);
    $$("#numberOfRequestsOK").append(stat.numberOfRequests.ok);
    $$("#numberOfRequestsKO").append(stat.numberOfRequests.ko);

    $$("#minResponseTime").append(stat.minResponseTime.total);
    $$("#minResponseTimeOK").append(stat.minResponseTime.ok);
    $$("#minResponseTimeKO").append(stat.minResponseTime.ko);

    $$("#maxResponseTime").append(stat.maxResponseTime.total);
    $$("#maxResponseTimeOK").append(stat.maxResponseTime.ok);
    $$("#maxResponseTimeKO").append(stat.maxResponseTime.ko);

    $$("#meanResponseTime").append(stat.meanResponseTime.total);
    $$("#meanResponseTimeOK").append(stat.meanResponseTime.ok);
    $$("#meanResponseTimeKO").append(stat.meanResponseTime.ko);

    $$("#standardDeviation").append(stat.standardDeviation.total);
    $$("#standardDeviationOK").append(stat.standardDeviation.ok);
    $$("#standardDeviationKO").append(stat.standardDeviation.ko);

    $$("#percentiles1").append(stat.percentiles1.total);
    $$("#percentiles1OK").append(stat.percentiles1.ok);
    $$("#percentiles1KO").append(stat.percentiles1.ko);

    $$("#percentiles2").append(stat.percentiles2.total);
    $$("#percentiles2OK").append(stat.percentiles2.ok);
    $$("#percentiles2KO").append(stat.percentiles2.ko);

    $$("#percentiles3").append(stat.percentiles3.total);
    $$("#percentiles3OK").append(stat.percentiles3.ok);
    $$("#percentiles3KO").append(stat.percentiles3.ko);

    $$("#percentiles4").append(stat.percentiles4.total);
    $$("#percentiles4OK").append(stat.percentiles4.ok);
    $$("#percentiles4KO").append(stat.percentiles4.ko);

    $$("#meanNumberOfRequestsPerSecond").append(stat.meanNumberOfRequestsPerSecond.total);
    $$("#meanNumberOfRequestsPerSecondOK").append(stat.meanNumberOfRequestsPerSecond.ok);
    $$("#meanNumberOfRequestsPerSecondKO").append(stat.meanNumberOfRequestsPerSecond.ko);
}
"""
  }
}

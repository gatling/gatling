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

import io.gatling.charts.component.RequestStatistics
import io.gatling.charts.report.{ GroupContainer, RequestContainer }
import io.gatling.charts.report.Container.{ GROUP, REQUEST }
import io.gatling.core.util.FileHelper.FileRichString
import io.gatling.core.util.HtmlHelper.HtmlRichString
import io.gatling.core.util.StringHelper.RichString

class StatsJsTemplate(stats: GroupContainer) {

  def getOutput: Fastring = {

      def renderStatsRequest(request: RequestStatistics) = {
        val jsonStats = new StatsJsonTemplate(request, false).getOutput

        fast"""name: "${request.name.escapeJsDoubleQuoteString}",
path: "${request.path.escapeJsDoubleQuoteString}",
pathFormatted: "${request.path.toFileName}",
stats: ${jsonStats}"""
      }

      def renderStatsGroup(group: GroupContainer): Fastring = fast"""type: "$GROUP",
contents: {
${
        (group.contents.values.map {
          _ match {
            case subGroup: GroupContainer => fast""""${subGroup.name.toFileName}": {
        ${renderStatsGroup(subGroup)}
    }"""
            case request: RequestContainer => fast""""${request.name.toFileName}": {
        type: "${REQUEST}",
        ${renderStatsRequest(request.stats)}
    }"""
          }
        }).mkFastring(",")
      }
},
${renderStatsRequest(group.stats)}
"""

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

    $$("#meanNumberOfRequestsPerSecond").append(stat.meanNumberOfRequestsPerSecond.total);
    $$("#meanNumberOfRequestsPerSecondOK").append(stat.meanNumberOfRequestsPerSecond.ok);
    $$("#meanNumberOfRequestsPerSecondKO").append(stat.meanNumberOfRequestsPerSecond.ko);
}
"""
  }
}

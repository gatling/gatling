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

import io.gatling.charts.component.RequestStatistics
import io.gatling.charts.report.{ GroupContainer, RequestContainer }
import io.gatling.charts.report.Container.{ GROUP, REQUEST }
import io.gatling.core.util.FileHelper.formatToFilename
import io.gatling.core.util.StringHelper.escapeJsDoubleQuoteString

class StatsJsTemplate(stats: GroupContainer) {

	def getOutput: String = {

		def renderStatsRequest(request: RequestStatistics) = {
			val jsonStats = new StatsJsonTemplate(request).getOutput
			fast"""
name: "${escapeJsDoubleQuoteString(request.name)}",
path: "${escapeJsDoubleQuoteString(request.path)}",
pathFormatted: "${formatToFilename(request.path)}",
stats: ${jsonStats}"""
		}

		def renderStatsGroup(group: GroupContainer): Fastring = fast"""
type: "$GROUP",
contents: {
${
			(group.contents.values.map {
				_ match {
					case subGroup: GroupContainer => renderStatsGroup(subGroup)
					case request: RequestContainer => fast""""${formatToFilename(request.name)}": {
        type: "${REQUEST}",
        ${renderStatsRequest(request.stats)}
    }"""
				}
			}).mkFastring(",")
		}
},
${renderStatsRequest(group.requestStats)}
"""

		fast"""
var stats = {
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
""".toString
	}
}

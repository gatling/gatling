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

class StatsJsonTemplate(stats: RequestStatistics) {
	def getOutput: String = fast"""{
    "name": "${stats.name}",
    "numberOfRequests": {
        "total": "${stats.numberOfRequestsStatistics.printableTotal}",
        "ok": "${stats.numberOfRequestsStatistics.printableSuccess}",
        "ko": "${stats.numberOfRequestsStatistics.printableFailure}"
    },
    "minResponseTime": {
        "total": "${stats.minResponseTimeStatistics.printableTotal}",
        "ok": "${stats.minResponseTimeStatistics.printableSuccess}",
        "ko": "${stats.minResponseTimeStatistics.printableFailure}"
    },
    "maxResponseTime": {
        "total": "${stats.maxResponseTimeStatistics.printableTotal}",
        "ok": "${stats.maxResponseTimeStatistics.printableSuccess}",
        "ko": "${stats.maxResponseTimeStatistics.printableFailure}"
    },
    "meanResponseTime": {
        "total": "${stats.meanStatistics.printableTotal}",
        "ok": "${stats.meanStatistics.printableSuccess}",
        "ko": "${stats.meanStatistics.printableFailure}"
    },
    "standardDeviation": {
        "total": "${stats.stdDeviationStatistics.printableTotal}",
        "ok": "${stats.stdDeviationStatistics.printableSuccess}",
        "ko": "${stats.stdDeviationStatistics.printableFailure}"
    },
    "percentiles1": {
        "total": "${stats.percentiles1.printableTotal}",
        "ok": "${stats.percentiles1.printableSuccess}",
        "ko": "${stats.percentiles1.printableFailure}"
    },
    "percentiles2": {
        "total": "${stats.percentiles2.printableTotal}",
        "ok": "${stats.percentiles2.printableSuccess}",
        "ko": "${stats.percentiles2.printableFailure}"
    },
    "group1": {
        "name": "${stats.groupedCounts(0).name}",
        "count": ${stats.groupedCounts(0).count},
        "percentage": ${stats.groupedCounts(0).percentage}
    },
    "group2": {
        "name": "${stats.groupedCounts(1).name}",
        "count": ${stats.groupedCounts(1).count},
        "percentage": ${stats.groupedCounts(1).percentage}
    },
    "group3": {
        "name": "${stats.groupedCounts(2).name}",
        "count": ${stats.groupedCounts(2).count},
        "percentage": ${stats.groupedCounts(2).percentage}
    },
    "group4": {
        "name": "${stats.groupedCounts(3).name}",
        "count": ${stats.groupedCounts(3).count},
        "percentage": ${stats.groupedCounts(3).percentage}
    },
    "meanNumberOfRequestsPerSecond": {
        "total": "${stats.meanNumberOfRequestsPerSecondStatistics.printableTotal}",
        "ok": "${stats.meanNumberOfRequestsPerSecondStatistics.printableSuccess}",
        "ko": "${stats.meanNumberOfRequestsPerSecondStatistics.printableFailure}"
    }
}""".toString
}
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
import io.gatling.core.util.FileHelper.formatToFilename

class StatsJsonTemplate(stats: RequestStatistics) {
	def getOutput: String = fast"""
{
        "name": "${stats.name}",
        "numberOfRequests": {
            "total": ${stats.numberOfRequestsStatistics.total},
            "ok": ${stats.numberOfRequestsStatistics.success},
            "ko": ${stats.numberOfRequestsStatistics.failure}
        },
        "minResponseTime": {
            "total": ${stats.minResponseTimeStatistics.total},
            "ok": ${stats.minResponseTimeStatistics.success},
            "ko": ${stats.minResponseTimeStatistics.failure}
        },
        "maxResponseTime": {
            "total": ${stats.maxResponseTimeStatistics.total},
            "ok": ${stats.maxResponseTimeStatistics.success},
            "ko": ${stats.maxResponseTimeStatistics.failure}
        },
        "meanResponseTime": {
            "total": ${stats.meanStatistics.total},
            "ok": ${stats.meanStatistics.success},
            "ko": ${stats.meanStatistics.failure}
        },
        "standardDeviation": {
            "total": ${stats.stdDeviationStatistics.total},
            "ok": ${stats.stdDeviationStatistics.success},
            "ko": ${stats.stdDeviationStatistics.failure}
        },
        "percentiles1": {
            "total": ${stats.percentiles1.total},
            "ok": ${stats.percentiles1.success},
            "ko": ${stats.percentiles1.failure}
        },
        "percentiles2": {
            "total": ${stats.percentiles2.total},
            "ok": ${stats.percentiles2.success},
            "ko": ${stats.percentiles2.failure}
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
            "total": ${stats.meanNumberOfRequestsPerSecondStatistics.total},
            "ok": ${stats.meanNumberOfRequestsPerSecondStatistics.success},
            "ko": ${stats.meanNumberOfRequestsPerSecondStatistics.failure}
        }
    }
	""".toString
}
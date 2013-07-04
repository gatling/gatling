/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import io.gatling.charts.component.Statistics.PrintableStat

class StatsJsonTemplate(stats: RequestStatistics, raw: Boolean) {

	def getOutput: Fastring = {

		def style(value: Long) =
			if (raw) // raw mode is used for JSON extract, non-raw for displaying in the reports
				value.toString
			else
				s""""${value.printable}""""

		fast"""{
    "name": "${stats.name}",
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
        "total": ${style(stats.meanNumberOfRequestsPerSecondStatistics.total)},
        "ok": ${style(stats.meanNumberOfRequestsPerSecondStatistics.success)},
        "ko": ${style(stats.meanNumberOfRequestsPerSecondStatistics.failure)}
    }
}"""
	}
}

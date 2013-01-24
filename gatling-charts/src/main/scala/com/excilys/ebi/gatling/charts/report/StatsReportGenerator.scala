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
package com.excilys.ebi.gatling.charts.report

import com.excilys.ebi.gatling.charts.component.{ ComponentLibrary, RequestStatistics, Statistics }
import com.excilys.ebi.gatling.charts.config.ChartsFiles.{ GLOBAL_PAGE_NAME, jsStatsFile, jsonStatsFile, tsvStatsFile }
import com.excilys.ebi.gatling.charts.template.{ StatsJsTemplate, StatsJsonTemplate, StatsTsvTemplate }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.{ RequestPath, Group }
import com.excilys.ebi.gatling.core.result.message.{ KO, OK }
import com.excilys.ebi.gatling.core.result.reader.DataReader

class StatsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) {

	def generate {

		def computeRequestStats(name: String, requestName: Option[String], group: Option[Group]): RequestStatistics = {
			val total = dataReader.generalStats(None, requestName, group)
			val ok = dataReader.generalStats(Some(OK), requestName, group)
			val ko = dataReader.generalStats(Some(KO), requestName, group)

			val numberOfRequestsStatistics = Statistics("numberOfRequests", total.count, ok.count, ko.count)
			val minResponseTimeStatistics = Statistics("minResponseTime", total.min, ok.min, ko.min)
			val maxResponseTimeStatistics = Statistics("maxResponseTime", total.max, ok.max, ko.max)
			val meanResponseTimeStatistics = Statistics("meanResponseTime", total.mean, ok.mean, ko.mean)
			val stdDeviationStatistics = Statistics("stdDeviation", total.stdDev, ok.stdDev, ko.stdDev)
			val percentiles1 = Statistics("percentiles1", total.percentile1, ok.percentile1, ko.percentile1)
			val percentiles2 = Statistics("percentiles2", total.percentile2, ok.percentile2, ko.percentile2)
			val meanNumberOfRequestsPerSecondStatistics = Statistics("meanNumberOfRequestsPerSecond", total.meanRequestsPerSec, ok.meanRequestsPerSec, ko.meanRequestsPerSec)

			val groupedCounts = dataReader
				.numberOfRequestInResponseTimeRange(requestName, group).map {
					case (name, count) => (name, count, count * 100 / total.count)
				}

			val path = requestName match {
				case Some(name) => RequestPath.path(name, group)
				case None => group.map(_.path).getOrElse("")
			}

			RequestStatistics(name, path, numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, meanResponseTimeStatistics, stdDeviationStatistics, percentiles1, percentiles2, groupedCounts, meanNumberOfRequestsPerSecondStatistics)
		}

		val stats = GroupContainer.root(computeRequestStats(GLOBAL_PAGE_NAME, None, None))

		dataReader.groupsAndRequests.foreach {
			case (group, Some(request)) => stats.addRequest(group, computeRequestStats(request, Some(request), group))
			case (Some(group), None) => stats.addGroup(group, computeRequestStats(group.name, None, Some(group)))
			case _ => throw new UnsupportedOperationException
		}

		new TemplateWriter(jsStatsFile(runOn)).writeToFile(new StatsJsTemplate(stats).getOutput)
		new TemplateWriter(jsonStatsFile(runOn)).writeToFile(new StatsJsonTemplate(stats.requestStats).getOutput)
		new TemplateWriter(tsvStatsFile(runOn)).writeToFile(new StatsTsvTemplate(stats).getOutput)
	}
}


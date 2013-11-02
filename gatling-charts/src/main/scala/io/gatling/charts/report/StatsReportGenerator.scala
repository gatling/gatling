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
package io.gatling.charts.report

import scala.annotation.tailrec

import io.gatling.charts.component.{ ComponentLibrary, GroupedCount, RequestStatistics, Statistics }
import io.gatling.charts.config.ChartsFiles.{ GLOBAL_PAGE_NAME, jsStatsFile, jsonStatsFile }
import io.gatling.charts.result.reader.RequestPath
import io.gatling.charts.template.{ ConsoleTemplate, StatsJsTemplate, StatsJsonTemplate }
import io.gatling.core.result.{ Group, GroupStatsPath, RequestStatsPath }
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.result.reader.DataReader

class StatsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) {

	def generate {

		def computeRequestStats(name: String, requestName: Option[String], group: Option[Group]): RequestStatistics = {

			val total = dataReader.requestGeneralStats(requestName, group, None)
			val ok = dataReader.requestGeneralStats(requestName, group, Some(OK))
			val ko = dataReader.requestGeneralStats(requestName, group, Some(KO))

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
					case (name, count) => GroupedCount(name, count, count * 100 / total.count)
				}

			val path = requestName match {
				case Some(name) => RequestPath.path(name, group)
				case None => group.map(RequestPath.path).getOrElse("")
			}

			RequestStatistics(name, path, numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, meanResponseTimeStatistics, stdDeviationStatistics, percentiles1, percentiles2, groupedCounts, meanNumberOfRequestsPerSecondStatistics)
		}

		def computeGroupStats(name: String, group: Group): RequestStatistics = {

			val total = dataReader.groupCumulatedResponseTimeGeneralStats(group, None)
			val ok = dataReader.groupCumulatedResponseTimeGeneralStats(group, Some(OK))
			val ko = dataReader.groupCumulatedResponseTimeGeneralStats(group, Some(KO))

			val numberOfRequestsStatistics = Statistics("numberOfRequests", total.count, ok.count, ko.count)
			val minResponseTimeStatistics = Statistics("minResponseTime", total.min, ok.min, ko.min)
			val maxResponseTimeStatistics = Statistics("maxResponseTime", total.max, ok.max, ko.max)
			val meanResponseTimeStatistics = Statistics("meanResponseTime", total.mean, ok.mean, ko.mean)
			val stdDeviationStatistics = Statistics("stdDeviation", total.stdDev, ok.stdDev, ko.stdDev)
			val percentiles1 = Statistics("percentiles1", total.percentile1, ok.percentile1, ko.percentile1)
			val percentiles2 = Statistics("percentiles2", total.percentile2, ok.percentile2, ko.percentile2)
			val meanNumberOfRequestsPerSecondStatistics = Statistics("meanNumberOfRequestsPerSecond", total.meanRequestsPerSec, ok.meanRequestsPerSec, ko.meanRequestsPerSec)

			val groupedCounts = dataReader
				.numberOfRequestInResponseTimeRange(None, Some(group)).map {
					case (name, count) => GroupedCount(name, count, count * 100 / total.count)
				}

			val path = RequestPath.path(group)

			RequestStatistics(name, path, numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, meanResponseTimeStatistics, stdDeviationStatistics, percentiles1, percentiles2, groupedCounts, meanNumberOfRequestsPerSecondStatistics)
		}

		val rootContainer = GroupContainer.root(computeRequestStats(GLOBAL_PAGE_NAME, None, None))

		val statsPaths = dataReader.statsPaths

		val groupStatsPaths = statsPaths.collect { case path: GroupStatsPath => path.group.hierarchy.reverse -> path }.toMap
		val seenGroups = collection.mutable.HashSet.empty[List[String]]

		@tailrec
		def addGroupsRec(hierarchy: List[String]) {

			if (!seenGroups.contains(hierarchy)) {
				seenGroups += hierarchy
				val group = groupStatsPaths(hierarchy).group
				val stats = computeGroupStats(group.name, group)
				rootContainer.addGroup(group, stats)

				hierarchy match {
					case head :: tail if !tail.isEmpty => addGroupsRec(tail)
					case _ =>
				}
			}
		}

		val requestStatsPaths = statsPaths.collect { case path: RequestStatsPath => path }
		requestStatsPaths.foreach {
			case RequestStatsPath(request, group) =>
				group.foreach { group =>
					addGroupsRec(group.hierarchy.reverse)
				}
				val stats = computeRequestStats(request, Some(request), group)
				rootContainer.addRequest(group, request, stats)
		}

		new TemplateWriter(jsStatsFile(runOn)).writeToFile(new StatsJsTemplate(rootContainer).getOutput)
		new TemplateWriter(jsonStatsFile(runOn)).writeToFile(new StatsJsonTemplate(rootContainer.stats, true).getOutput)
		println(ConsoleTemplate(rootContainer.stats))
	}
}


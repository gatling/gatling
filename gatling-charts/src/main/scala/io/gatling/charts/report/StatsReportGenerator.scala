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
package io.gatling.charts.report

import scala.collection.breakOut

import io.gatling.charts.component.{ ComponentLibrary, GroupedCount, RequestStatistics, Statistics }
import io.gatling.charts.config.ChartsFiles._
import io.gatling.charts.result.reader.RequestPath
import io.gatling.charts.template.{ ConsoleTemplate, StatsJsTemplate, GlobalStatsJsonTemplate }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.{ Group, GroupStatsPath, RequestStatsPath }
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.util.NumberHelper._

private[charts] class StatsReportGenerator(reportsGenerationInputs: ReportsGenerationInputs, componentLibrary: ComponentLibrary)(implicit configuration: GatlingConfiguration) {

  import reportsGenerationInputs._

  def generate(): Unit = {

      def computeRequestStats(name: String, requestName: Option[String], group: Option[Group]): RequestStatistics = {

        val total = dataReader.requestGeneralStats(requestName, group, None)
        val ok = dataReader.requestGeneralStats(requestName, group, Some(OK))
        val ko = dataReader.requestGeneralStats(requestName, group, Some(KO))

        val numberOfRequestsStatistics = Statistics("request count", total.count, ok.count, ko.count)
        val minResponseTimeStatistics = Statistics("min response time", total.min, ok.min, ko.min)
        val maxResponseTimeStatistics = Statistics("max response time", total.max, ok.max, ko.max)
        val meanResponseTimeStatistics = Statistics("mean response time", total.mean, ok.mean, ko.mean)
        val stdDeviationStatistics = Statistics("std deviation", total.stdDev, ok.stdDev, ko.stdDev)
        val percentiles1 = Statistics(s"response time ${configuration.charting.indicators.percentile1.toRank} percentile", total.percentile1, ok.percentile1, ko.percentile1)
        val percentiles2 = Statistics(s"response time ${configuration.charting.indicators.percentile2.toRank} percentile", total.percentile2, ok.percentile2, ko.percentile2)
        val percentiles3 = Statistics(s"response time ${configuration.charting.indicators.percentile3.toRank} percentile", total.percentile3, ok.percentile3, ko.percentile3)
        val percentiles4 = Statistics(s"response time ${configuration.charting.indicators.percentile4.toRank} percentile", total.percentile4, ok.percentile4, ko.percentile4)
        val meanNumberOfRequestsPerSecondStatistics = Statistics("mean requests/sec", total.meanRequestsPerSec, ok.meanRequestsPerSec, ko.meanRequestsPerSec)

        val groupedCounts = dataReader
          .numberOfRequestInResponseTimeRange(requestName, group).map {
            case (rangeName, count) => GroupedCount(rangeName, count, math.round(count * 100.0f / total.count))
          }

        val path = requestName match {
          case Some(n) => RequestPath.path(n, group)
          case None    => group.map(RequestPath.path).getOrElse("")
        }

        RequestStatistics(name, path, numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, meanResponseTimeStatistics, stdDeviationStatistics, percentiles1, percentiles2, percentiles3, percentiles4, groupedCounts, meanNumberOfRequestsPerSecondStatistics)
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
        val percentiles3 = Statistics("percentiles3", total.percentile3, ok.percentile3, ko.percentile3)
        val percentiles4 = Statistics("percentiles4", total.percentile4, ok.percentile4, ko.percentile4)
        val meanNumberOfRequestsPerSecondStatistics = Statistics("meanNumberOfRequestsPerSecond", total.meanRequestsPerSec, ok.meanRequestsPerSec, ko.meanRequestsPerSec)

        val groupedCounts = dataReader
          .numberOfRequestInResponseTimeRange(None, Some(group)).map {
            case (rangeName, count) => if (total.count != 0) GroupedCount(rangeName, count, count * 100 / total.count) else GroupedCount(rangeName, count, 0)
          }

        val path = RequestPath.path(group)

        RequestStatistics(name, path, numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, meanResponseTimeStatistics, stdDeviationStatistics, percentiles1, percentiles2, percentiles3, percentiles4, groupedCounts, meanNumberOfRequestsPerSecondStatistics)
      }

    val rootContainer = GroupContainer.root(computeRequestStats(GlobalPageName, None, None))

    val statsPaths = dataReader.statsPaths

    val groupsByHierarchy: Map[List[String], Group] = statsPaths
      .collect {
        case GroupStatsPath(group)            => group
        case RequestStatsPath(_, Some(group)) => group
      }.map(group => group.hierarchy.reverse -> group)(breakOut)

    val seenGroups = collection.mutable.HashSet.empty[List[String]]

      def addGroupsRec(hierarchy: List[String]): Unit = {

        if (!seenGroups.contains(hierarchy)) {
          seenGroups += hierarchy

          hierarchy match {
            case head :: tail if tail.nonEmpty => addGroupsRec(tail)
            case _                             =>
          }

          val group = groupsByHierarchy(hierarchy)
          val stats = computeGroupStats(group.name, group)
          rootContainer.addGroup(group, stats)
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

    new TemplateWriter(statsJsFile(reportFolderName)).writeToFile(new StatsJsTemplate(rootContainer).getOutput(configuration.core.charset))
    new TemplateWriter(globalStatsJsonFile(reportFolderName)).writeToFile(new GlobalStatsJsonTemplate(rootContainer.stats, true).getOutput)
    println(ConsoleTemplate(dataReader, rootContainer.stats))
  }
}


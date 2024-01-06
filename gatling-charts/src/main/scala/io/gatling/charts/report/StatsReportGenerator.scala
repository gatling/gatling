/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.charts.report

import java.nio.charset.Charset

import io.gatling.charts.component.{ RequestStatistics, Stats }
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.config.ChartsFiles.AllRequestLineTitle
import io.gatling.charts.stats.{ GeneralStats, Group, GroupStatsPath, RequestPath, RequestStatsPath }
import io.gatling.charts.template.{ ConsoleTemplate, GlobalStatsJsonTemplate, StatsJsTemplate }
import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.core.config.ReportsConfiguration
import io.gatling.shared.util.NumberHelper._

import com.typesafe.scalalogging.StrictLogging

private[charts] class StatsReportGenerator(
    reportsGenerationInputs: ReportsGenerationInputs,
    chartsFiles: ChartsFiles,
    charset: Charset,
    configuration: ReportsConfiguration
) extends StrictLogging {
  import reportsGenerationInputs._

  def generate(): Unit = {
    def percentiles(rank: Double, title: Double => String, total: GeneralStats, ok: GeneralStats, ko: GeneralStats) =
      new Stats(title(rank), total.percentile(rank), ok.percentile(rank), ko.percentile(rank))

    def computeRequestStats(name: String, requestName: Option[String], group: Option[Group]): RequestStatistics = {
      val total = logFileData.requestGeneralStats(requestName, group, None)
      val ok = logFileData.requestGeneralStats(requestName, group, Some(OK))
      val ko = logFileData.requestGeneralStats(requestName, group, Some(KO))

      val percentilesTitle = (rank: Double) => s"response time ${rank.toRank} percentile"

      val path = requestName match {
        case Some(n) => RequestPath.path(n, group)
        case _       => group.map(RequestPath.path).getOrElse("")
      }

      new RequestStatistics(
        name,
        path,
        numberOfRequestsStatistics = new Stats("request count", total.count, ok.count, ko.count),
        minResponseTimeStatistics = new Stats("min response time", total.min, ok.min, ko.min),
        maxResponseTimeStatistics = new Stats("max response time", total.max, ok.max, ko.max),
        meanResponseTimeStatistics = new Stats("mean response time", total.mean, ok.mean, ko.mean),
        stdDeviationStatistics = new Stats("std deviation", total.stdDev, ok.stdDev, ko.stdDev),
        percentiles1 = percentiles(configuration.indicators.percentile1, percentilesTitle, total, ok, ko),
        percentiles2 = percentiles(configuration.indicators.percentile2, percentilesTitle, total, ok, ko),
        percentiles3 = percentiles(configuration.indicators.percentile3, percentilesTitle, total, ok, ko),
        percentiles4 = percentiles(configuration.indicators.percentile4, percentilesTitle, total, ok, ko),
        ranges = logFileData.numberOfRequestInResponseTimeRanges(requestName, group),
        meanNumberOfRequestsPerSecondStatistics = new Stats("mean requests/sec", total.meanRequestsPerSec, ok.meanRequestsPerSec, ko.meanRequestsPerSec)
      )
    }

    def computeGroupStats(name: String, group: Group): RequestStatistics = {
      def groupStatsFunction: (Group, Option[Status]) => GeneralStats =
        if (configuration.useGroupDurationMetric) {
          logger.debug("Use group duration stats.")
          logFileData.groupDurationGeneralStats
        } else {
          logger.debug("Use group cumulated response time stats.")
          logFileData.groupCumulatedResponseTimeGeneralStats
        }

      val total = groupStatsFunction(group, None)
      val ok = groupStatsFunction(group, Some(OK))
      val ko = groupStatsFunction(group, Some(KO))

      new RequestStatistics(
        name,
        path = RequestPath.path(group),
        numberOfRequestsStatistics = new Stats("numberOfRequests", total.count, ok.count, ko.count),
        minResponseTimeStatistics = new Stats("minResponseTime", total.min, ok.min, ko.min),
        maxResponseTimeStatistics = new Stats("maxResponseTime", total.max, ok.max, ko.max),
        meanResponseTimeStatistics = new Stats("meanResponseTime", total.mean, ok.mean, ko.mean),
        stdDeviationStatistics = new Stats("stdDeviation", total.stdDev, ok.stdDev, ko.stdDev),
        percentiles1 = percentiles(configuration.indicators.percentile1, _ => "percentiles1", total, ok, ko),
        percentiles2 = percentiles(configuration.indicators.percentile2, _ => "percentiles2", total, ok, ko),
        percentiles3 = percentiles(configuration.indicators.percentile3, _ => "percentiles3", total, ok, ko),
        percentiles4 = percentiles(configuration.indicators.percentile4, _ => "percentiles4", total, ok, ko),
        ranges = logFileData.numberOfRequestInResponseTimeRanges(None, Some(group)),
        meanNumberOfRequestsPerSecondStatistics =
          new Stats("meanNumberOfRequestsPerSecond", total.meanRequestsPerSec, ok.meanRequestsPerSec, ko.meanRequestsPerSec)
      )
    }

    val rootContainer = GroupContainer.root(computeRequestStats(AllRequestLineTitle, None, None))

    val statsPaths = logFileData.statsPaths

    val groupsByHierarchy: Map[List[String], Group] = statsPaths
      .collect {
        case GroupStatsPath(group)            => group
        case RequestStatsPath(_, Some(group)) => group
      }
      .map(group => group.hierarchy.reverse -> group)
      .toMap

    val seenGroups = collection.mutable.HashSet.empty[List[String]]

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def addGroupsRec(hierarchy: List[String]): Unit =
      if (!seenGroups.contains(hierarchy)) {
        seenGroups += hierarchy

        hierarchy match {
          case _ :: tail if tail.nonEmpty => addGroupsRec(tail)
          case _                          =>
        }

        val group = groupsByHierarchy(hierarchy)
        val stats = computeGroupStats(group.name, group)
        rootContainer.addGroup(group, stats)
      }

    val requestStatsPaths = statsPaths.collect { case path: RequestStatsPath => path }
    requestStatsPaths.foreach { case RequestStatsPath(request, group) =>
      group.foreach { group =>
        addGroupsRec(group.hierarchy.reverse)
      }
      val stats = computeRequestStats(request, Some(request), group)
      rootContainer.addRequest(group, request, stats)
    }

    new TemplateWriter(chartsFiles.statsJsFile).writeToFile(new StatsJsTemplate(rootContainer, false).getOutput, charset)
    new TemplateWriter(chartsFiles.statsJsonFile).writeToFile(new StatsJsTemplate(rootContainer, true).getOutput, charset)
    new TemplateWriter(chartsFiles.globalStatsJsonFile).writeToFile(new GlobalStatsJsonTemplate(rootContainer.stats, true).getOutput, charset)
    println(ConsoleTemplate.println(rootContainer.stats, logFileData.errors(None, None)))
  }
}

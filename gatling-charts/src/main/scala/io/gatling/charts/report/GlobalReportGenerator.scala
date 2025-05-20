/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
import java.time.ZoneId

import io.gatling.charts.component._
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.stats._
import io.gatling.charts.template.GlobalPageTemplate
import io.gatling.charts.util.Color
import io.gatling.commons.stats.OK
import io.gatling.commons.util.Collections._
import io.gatling.core.config.ReportsConfiguration
import io.gatling.shared.model.assertion.AssertionResult

private[charts] final class GlobalReportGenerator(
    logFileData: LogFileData,
    assertionResults: List[AssertionResult],
    rootContainer: GroupContainer,
    chartsFiles: ChartsFiles,
    componentLibrary: ComponentLibrary,
    zoneId: ZoneId,
    charset: Charset,
    configuration: ReportsConfiguration
) extends ReportGenerator {

  private def userStartRateComponent(logFileData: LogFileData) = {
    val seriesColors = Iterator.continually(Color.Users.Base).flatten.take(logFileData.scenarioNames.size).toList

    val userStartRateSeries: Seq[Series[IntVsTimePlot]] = logFileData.scenarioNames
      .map { scenarioName =>
        scenarioName -> logFileData.userStartRatePerSecond(Some(scenarioName))
      }
      .reverse
      .zip(seriesColors)
      .map { case ((scenarioName, data), color) => new Series[IntVsTimePlot](scenarioName, data, List(color)) } ::: List(
      new Series[IntVsTimePlot]("All users", logFileData.userStartRatePerSecond(None), List(Color.Users.All))
    )

    componentLibrary.getUserStartRateComponent(logFileData.runInfo.injectStart, userStartRateSeries)
  }

  private def maxNumberOfConcurrentUsersComponent(logFileData: LogFileData) = {
    val seriesColors = Iterator.continually(Color.Users.Base).flatten.take(logFileData.scenarioNames.size).toList

    val userStartRateSeries: Seq[Series[IntVsTimePlot]] = logFileData.scenarioNames
      .map { scenarioName =>
        scenarioName -> logFileData.maxNumberOfConcurrentUsersPerSecond(Some(scenarioName))
      }
      .reverse
      .zip(seriesColors)
      .map { case ((scenarioName, data), color) => new Series[IntVsTimePlot](scenarioName, data, List(color)) } ::: List(
      new Series[IntVsTimePlot]("All users", logFileData.maxNumberOfConcurrentUsersPerSecond(None), List(Color.Users.All))
    )

    componentLibrary.getMaxConcurrentUsersComponent(logFileData.runInfo.injectStart, userStartRateSeries)
  }

  private def responseTimeDistributionChartComponent(logFileData: LogFileData): Component = {
    val (okDistribution, koDistribution) = logFileData.responseTimeDistribution(100, None, None)
    val okDistributionSeries = new Series(Series.OK, okDistribution, List(Color.Requests.Ok))
    val koDistributionSeries = new Series(Series.KO, koDistribution, List(Color.Requests.Ko))

    componentLibrary.getDistributionComponent("Response Time", "Requests", okDistributionSeries, koDistributionSeries)
  }

  private def responseTimeChartComponent(logFileData: LogFileData): Component = {
    val successData = logFileData.responseTimePercentilesOverTime(OK, None, None)
    val successSeries = new Series[PercentilesVsTimePlot](s"Response Time Percentiles over Time (${Series.OK})", successData, Color.Requests.Percentiles)

    componentLibrary.getPercentilesOverTimeComponent("Response Time", logFileData.runInfo.injectStart, successSeries)
  }

  private def requestsChartComponent(logFileData: LogFileData): Component =
    countsChartComponent(logFileData.numberOfRequestsPerSecond, componentLibrary.getRequestsComponent, logFileData.runInfo.injectStart)

  private def responsesChartComponent(logFileData: LogFileData): Component =
    countsChartComponent(logFileData.numberOfResponsesPerSecond, componentLibrary.getResponsesComponent, logFileData.runInfo.injectStart)

  private def countsChartComponent(
      dataSource: (Option[String], Option[Group]) => Seq[CountsVsTimePlot],
      componentFactory: (Long, Series[CountsVsTimePlot], Series[PieSlice]) => Component,
      injectStart: Long
  ): Component = {
    val counts = dataSource(None, None).sortBy(_.time)

    val countsSeries = new Series[CountsVsTimePlot]("", counts, List(Color.Requests.All, Color.Requests.Ok, Color.Requests.Ko))
    val pieRequestsSeries = new Series[PieSlice](
      Series.Distribution,
      List(
        new PieSlice(Series.OK, counts.sumBy(_.oks)),
        new PieSlice(Series.KO, counts.sumBy(_.kos))
      ),
      List(Color.Requests.Ok, Color.Requests.Ko)
    )

    componentFactory(injectStart, countsSeries, pieRequestsSeries)
  }

  def generate(): Unit = {
    val ranges = logFileData.numberOfRequestInResponseTimeRanges(None, None)

    val template = new GlobalPageTemplate(
      logFileData.runInfo,
      rootContainer,
      new SchemaContainerComponent(
        componentLibrary.getRangesComponent("Response Time Ranges", "requests", ranges, large = false),
        componentLibrary.getRequestCountPolarComponent(rootContainer),
        new SimulationCardComponent(logFileData.runInfo, zoneId)
      ),
      new AssertionsTableComponent(assertionResults),
      new GlobalStatsTableComponent(rootContainer, configuration.indicators),
      new ErrorsTableComponent(logFileData.errors(None, None)),
      userStartRateComponent(logFileData),
      maxNumberOfConcurrentUsersComponent(logFileData),
      responseTimeDistributionChartComponent(logFileData),
      responseTimeChartComponent(logFileData),
      requestsChartComponent(logFileData),
      responsesChartComponent(logFileData)
    )

    new TemplateWriter(chartsFiles.globalFile).writeToFile(template.getOutput, charset)
  }
}

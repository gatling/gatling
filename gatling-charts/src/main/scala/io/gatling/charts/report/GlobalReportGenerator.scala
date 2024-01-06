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
import java.time.ZoneId

import io.gatling.charts.component._
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.stats._
import io.gatling.charts.template.GlobalPageTemplate
import io.gatling.charts.util.Color
import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.core.config.ReportsConfiguration

private[charts] class GlobalReportGenerator(
    reportsGenerationInputs: ReportsGenerationInputs,
    chartsFiles: ChartsFiles,
    componentLibrary: ComponentLibrary,
    zoneId: ZoneId,
    charset: Charset,
    configuration: ReportsConfiguration
) extends ReportGenerator {
  def generate(): Unit = {
    import reportsGenerationInputs._

    def activeSessionsChartComponent = {
      val seriesColors = Iterator.continually(Color.Users.Base).flatten.take(logFileData.scenarioNames.size).toList

      val activeSessionsSeries: Seq[Series[IntVsTimePlot]] = logFileData.scenarioNames
        .map { scenarioName =>
          scenarioName -> logFileData.numberOfActiveSessionsPerSecond(Some(scenarioName))
        }
        .reverse
        .zip(seriesColors)
        .map { case ((scenarioName, data), color) => new Series[IntVsTimePlot](scenarioName, data, List(color)) }

      componentLibrary.getActiveSessionsComponent(logFileData.runInfo.injectStart, activeSessionsSeries)
    }

    def responseTimeDistributionChartComponent: Component = {
      val (okDistribution, koDistribution) = logFileData.responseTimeDistribution(100, None, None)
      val okDistributionSeries = new Series(Series.OK, okDistribution, List(Color.Requests.Ok))
      val koDistributionSeries = new Series(Series.KO, koDistribution, List(Color.Requests.Ko))

      componentLibrary.getDistributionComponent("Response Time", "Requests", okDistributionSeries, koDistributionSeries)
    }

    def responseTimeChartComponent: Component =
      percentilesChartComponent(
        logFileData.responseTimePercentilesOverTime,
        componentLibrary.getPercentilesOverTimeComponent("Response Time", _, _),
        "Response Time Percentiles over Time"
      )

    def percentilesChartComponent(
        dataSource: (Status, Option[String], Option[Group]) => Iterable[PercentilesVsTimePlot],
        componentFactory: (Long, Series[PercentilesVsTimePlot]) => Component,
        title: String
    ): Component = {
      val successData = dataSource(OK, None, None)
      val successSeries = new Series[PercentilesVsTimePlot](s"$title (${Series.OK})", successData, Color.Requests.Percentiles)

      componentFactory(logFileData.runInfo.injectStart, successSeries)
    }

    def requestsChartComponent: Component =
      countsChartComponent(logFileData.numberOfRequestsPerSecond, componentLibrary.getRequestsComponent)

    def responsesChartComponent: Component =
      countsChartComponent(logFileData.numberOfResponsesPerSecond, componentLibrary.getResponsesComponent)

    def countsChartComponent(
        dataSource: (Option[String], Option[Group]) => Seq[CountsVsTimePlot],
        componentFactory: (Long, Series[CountsVsTimePlot], Series[PieSlice]) => Component
    ): Component = {
      val counts = dataSource(None, None).sortBy(_.time)

      val countsSeries = new Series[CountsVsTimePlot]("", counts, List(Color.Requests.All, Color.Requests.Ok, Color.Requests.Ko))
      val pieRequestsSeries = new Series[PieSlice](
        Series.Distribution,
        List(new PieSlice(Series.OK, count(counts, OK)), new PieSlice(Series.KO, count(counts, KO))),
        List(Color.Requests.Ok, Color.Requests.Ko)
      )

      componentFactory(logFileData.runInfo.injectStart, countsSeries, pieRequestsSeries)
    }

    val template = new GlobalPageTemplate(
      logFileData.runInfo,
      new SchemaContainerComponent(
        componentLibrary.getRangesComponent("Response Time Ranges", "requests", large = false),
        componentLibrary.getRequestCountPolarComponent,
        new SimulationCardComponent(logFileData.runInfo, zoneId)
      ),
      new AssertionsTableComponent(assertionResults),
      new GlobalStatsTableComponent(configuration.indicators),
      new ErrorsTableComponent(logFileData.errors(None, None)),
      activeSessionsChartComponent,
      responseTimeDistributionChartComponent,
      responseTimeChartComponent,
      requestsChartComponent,
      responsesChartComponent
    )

    new TemplateWriter(chartsFiles.globalFile).writeToFile(template.getOutput, charset)
  }
}

/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import io.gatling.charts.component._
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.stats._
import io.gatling.charts.template.RequestDetailsPageTemplate
import io.gatling.charts.util.Colors._
import io.gatling.commons.shared.unstable.model.stats.{ Group, RequestStatsPath }
import io.gatling.commons.stats._
import io.gatling.core.config.GatlingConfiguration

private[charts] class RequestDetailsReportGenerator(
    reportsGenerationInputs: ReportsGenerationInputs,
    chartsFiles: ChartsFiles,
    componentLibrary: ComponentLibrary
)(implicit
    configuration: GatlingConfiguration
) extends ReportGenerator {

  def generate(): Unit = {
    import reportsGenerationInputs._

    def generateDetailPage(path: String, requestName: String, group: Option[Group]): Unit = {

      def responseTimeDistributionChartComponent: Component = {
        val (okDistribution, koDistribution) = logFileReader.responseTimeDistribution(100, Some(requestName), group)
        val okDistributionSeries = new Series(Series.OK, okDistribution, List(Blue))
        val koDistributionSeries = new Series(Series.KO, koDistribution, List(Red))

        componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
      }

      def responseTimeChartComponent: Component =
        percentilesChartComponent(
          logFileReader.responseTimePercentilesOverTime,
          componentLibrary.getRequestDetailsResponseTimeChartComponent,
          "Response Time Percentiles over Time"
        )

      def percentilesChartComponent(
          dataSource: (Status, Option[String], Option[Group]) => Iterable[PercentilesVsTimePlot],
          componentFactory: (Long, Series[PercentilesVsTimePlot]) => Component,
          title: String
      ): Component = {
        val successData = dataSource(OK, Some(requestName), group)
        val successSeries = new Series[PercentilesVsTimePlot](s"$title (${Series.OK})", successData, ReportGenerator.PercentilesColors)

        componentFactory(logFileReader.runStart, successSeries)
      }

      def requestsChartComponent: Component =
        countsChartComponent(logFileReader.numberOfRequestsPerSecond, componentLibrary.getRequestsChartComponent)

      def responsesChartComponent: Component =
        countsChartComponent(logFileReader.numberOfResponsesPerSecond, componentLibrary.getResponsesChartComponent)

      def countsChartComponent(
          dataSource: (Option[String], Option[Group]) => Seq[CountsVsTimePlot],
          componentFactory: (Long, Series[CountsVsTimePlot], Series[PieSlice]) => Component
      ): Component = {

        val counts = dataSource(Some(requestName), group).sortBy(_.time)

        val countsSeries = new Series[CountsVsTimePlot]("", counts, List(Blue, Red, Green))
        val okPieSlice = new PieSlice(Series.OK, count(counts, OK))
        val koPieSlice = new PieSlice(Series.KO, count(counts, KO))
        val pieRequestsSeries = new Series[PieSlice](Series.Distribution, Seq(okPieSlice, koPieSlice), List(Green, Red))

        componentFactory(logFileReader.runStart, countsSeries, pieRequestsSeries)
      }

      def responseTimeScatterChartComponent: Component =
        scatterChartComponent(
          logFileReader.responseTimeAgainstGlobalNumberOfRequestsPerSec,
          componentLibrary.getRequestDetailsResponseTimeScatterChartComponent
        )

      def scatterChartComponent(
          dataSource: (Status, String, Option[Group]) => Seq[IntVsTimePlot],
          componentFactory: (Series[IntVsTimePlot], Series[IntVsTimePlot]) => Component
      ): Component = {

        val scatterPlotSuccessData = dataSource(OK, requestName, group)
        val scatterPlotFailuresData = dataSource(KO, requestName, group)
        val scatterPlotSuccessSeries = new Series[IntVsTimePlot](Series.OK, scatterPlotSuccessData, List(TranslucidBlue))
        val scatterPlotFailuresSeries = new Series[IntVsTimePlot](Series.KO, scatterPlotFailuresData, List(TranslucidRed))

        componentFactory(scatterPlotSuccessSeries, scatterPlotFailuresSeries)
      }

      val template =
        new RequestDetailsPageTemplate(
          path,
          requestName,
          group,
          new StatisticsTextComponent,
          componentLibrary.getRequestDetailsIndicatorChartComponent,
          new ErrorsTableComponent(logFileReader.errors(Some(requestName), group)),
          responseTimeDistributionChartComponent,
          responseTimeChartComponent,
          requestsChartComponent,
          responsesChartComponent,
          responseTimeScatterChartComponent
        )

      new TemplateWriter(chartsFiles.requestFile(path)).writeToFile(template.getOutput(configuration.core.charset))
    }

    logFileReader.statsPaths.foreach {
      case RequestStatsPath(request, group) => generateDetailPage(RequestPath.path(request, group), request, group)
      case _                                =>
    }
  }
}

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

import io.gatling.charts.component._
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.stats._
import io.gatling.charts.template.DetailsPageTemplate
import io.gatling.charts.util.Color
import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.Collections._
import io.gatling.core.config.ReportsConfiguration

private[charts] class RequestDetailsReportGenerator(
    logFileData: LogFileData,
    rootContainer: GroupContainer,
    chartsFiles: ChartsFiles,
    componentLibrary: ComponentLibrary,
    charset: Charset,
    configuration: ReportsConfiguration
) extends ReportGenerator {
  def generate(): Unit = {
    def generateDetailPage(requestContainer: RequestContainer): Unit = {
      def responseTimeDistributionChartComponent: Component = {
        val (okDistribution, koDistribution) = logFileData.responseTimeDistribution(100, Some(requestContainer.name), requestContainer.group)
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
        val successData = dataSource(OK, Some(requestContainer.name), requestContainer.group)
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
        val counts = dataSource(Some(requestContainer.name), requestContainer.group).sortBy(_.time)

        val countsSeries = new Series[CountsVsTimePlot]("", counts, List(Color.Requests.All, Color.Requests.Ok, Color.Requests.Ko))
        val pieRequestsSeries = new Series[PieSlice](
          Series.Distribution,
          List(
            new PieSlice(Series.OK, counts.sumBy(_.oks)),
            new PieSlice(Series.KO, counts.sumBy(_.kos))
          ),
          List(Color.Requests.Ok, Color.Requests.Ko)
        )

        componentFactory(logFileData.runInfo.injectStart, countsSeries, pieRequestsSeries)
      }

      def responseTimeScatterChartComponent: Component =
        scatterChartComponent(
          logFileData.responseTimeAgainstGlobalNumberOfRequestsPerSec,
          componentLibrary.getResponseTimeScatterComponent
        )

      def scatterChartComponent(
          dataSource: (Status, String, Option[Group]) => Seq[IntVsTimePlot],
          componentFactory: (Series[IntVsTimePlot], Series[IntVsTimePlot]) => Component
      ): Component = {
        val scatterPlotSuccessData = dataSource(OK, requestContainer.name, requestContainer.group)
        val scatterPlotFailuresData = dataSource(KO, requestContainer.name, requestContainer.group)
        val scatterPlotSuccessSeries = new Series[IntVsTimePlot](Series.OK, scatterPlotSuccessData, List(Color.Requests.Ok))
        val scatterPlotFailuresSeries = new Series[IntVsTimePlot](Series.KO, scatterPlotFailuresData, List(Color.Requests.Ko))

        componentFactory(scatterPlotSuccessSeries, scatterPlotFailuresSeries)
      }

      val ranges = logFileData.numberOfRequestInResponseTimeRanges(Some(requestContainer.name), requestContainer.group)

      val path = RequestPath.path(requestContainer.name, requestContainer.group)

      val template =
        new DetailsPageTemplate(
          logFileData.runInfo,
          path,
          requestContainer,
          rootContainer,
          new SchemaContainerComponent(
            componentLibrary.getRangesComponent("Response Time Ranges", "requests", ranges, large = true),
            new DetailsStatsTableComponent(requestContainer.stats, configuration.indicators)
          ),
          new ErrorsTableComponent(logFileData.errors(Some(requestContainer.name), requestContainer.group)),
          responseTimeDistributionChartComponent,
          responseTimeChartComponent,
          requestsChartComponent,
          responsesChartComponent,
          responseTimeScatterChartComponent
        )

      new TemplateWriter(chartsFiles.requestFile(path)).writeToFile(template.getOutput, charset)
    }

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def generateDetailPageRec(groupContainer: GroupContainer): Unit = {
      groupContainer.requests.values.foreach { requestContainer =>
        generateDetailPage(requestContainer)
      }
      groupContainer.groups.values.foreach(generateDetailPageRec)
    }

    generateDetailPageRec(rootContainer)
  }
}

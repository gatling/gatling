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

import io.gatling.charts.component._
import io.gatling.charts.config.ChartsFiles.requestFile
import io.gatling.charts.result.reader.RequestPath
import io.gatling.charts.template.RequestDetailsPageTemplate
import io.gatling.charts.util.Colors._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result._
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.result.message.Status

private[charts] class RequestDetailsReportGenerator(reportsGenerationInputs: ReportsGenerationInputs, componentLibrary: ComponentLibrary)(implicit configuration: GatlingConfiguration)
    extends ReportGenerator {

  def generate(): Unit = {
    import reportsGenerationInputs._

      def generateDetailPage(path: String, requestName: String, group: Option[Group]): Unit = {

          def responseTimeDistributionChartComponent: Component = {
            val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100, Some(requestName), group)
            val okDistributionSeries = new Series(Series.OK, okDistribution, List(Blue))
            val koDistributionSeries = new Series(Series.KO, koDistribution, List(Red))

            componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
          }

          def responseTimeChartComponent: Component = {
            val responseTimesPercentilesSuccessData = dataReader.responseTimePercentilesOverTime(OK, Some(requestName), group)

            val responseTimesSuccessSeries = new Series[PercentilesVsTimePlot](s"Response Time Percentiles over Time (${Series.OK})", responseTimesPercentilesSuccessData, ReportGenerator.PercentilesColors)

            componentLibrary.getRequestDetailsResponseTimeChartComponent(dataReader.runStart, responseTimesSuccessSeries)
          }

          def requestsChartComponent: Component = {
            val counts = dataReader.numberOfRequestsPerSecond(Some(requestName), None).sortBy(_.time)

            val countsSeries = new Series[CountsVsTimePlot]("", counts, List(Blue, Red, Green))
            val pieRequestsSeries = new Series[PieSlice](Series.Distribution, PieSlice(Series.OK, count(counts, OK)) :: PieSlice(Series.KO, count(counts, KO)) :: Nil, List(Green, Red))

            componentLibrary.getRequestsChartComponent(dataReader.runStart, countsSeries, pieRequestsSeries)
          }

          def responsesChartComponent: Component = {
            val counts = dataReader.numberOfResponsesPerSecond(Some(requestName), group).sortBy(_.time)

            val countsSeries = new Series[CountsVsTimePlot]("", counts, List(Blue, Red, Green))
            val pieRequestsSeries = new Series[PieSlice](Series.Distribution, PieSlice(Series.OK, count(counts, OK)) :: PieSlice(Series.KO, count(counts, KO)) :: Nil, List(Green, Red))

            componentLibrary.getResponsesChartComponent(dataReader.runStart, countsSeries, pieRequestsSeries)
          }

          def latencyChartComponent: Component = {

            val latencyPercentilesSuccessData = dataReader.latencyPercentilesOverTime(OK, Some(requestName), group)

            val latencySuccessSeries = new Series[PercentilesVsTimePlot](s"Latency Percentiles over Time (${Series.OK})", latencyPercentilesSuccessData, ReportGenerator.PercentilesColors)

            componentLibrary.getRequestDetailsLatencyChartComponent(dataReader.runStart, latencySuccessSeries)
          }

          def scatterChartComponent(datasource: (Status, String, Option[Group]) => Seq[IntVsTimePlot],
                                    componentFactory: (Series[IntVsTimePlot], Series[IntVsTimePlot]) => Component): Component = {

            val scatterPlotSuccessData = datasource(OK, requestName, group)
            val scatterPlotFailuresData = datasource(KO, requestName, group)
            val scatterPlotSuccessSeries = new Series[IntVsTimePlot](Series.OK, scatterPlotSuccessData, List(TranslucidBlue))
            val scatterPlotFailuresSeries = new Series[IntVsTimePlot](Series.KO, scatterPlotFailuresData, List(TranslucidRed))

            componentFactory(scatterPlotSuccessSeries, scatterPlotFailuresSeries)
          }

          def responseTimeScatterChartComponent: Component =
            scatterChartComponent(dataReader.responseTimeAgainstGlobalNumberOfRequestsPerSec, componentLibrary.getRequestDetailsResponseTimeScatterChartComponent)

          def latencyScatterChartComponent: Component =
            scatterChartComponent(dataReader.latencyAgainstGlobalNumberOfRequestsPerSec, componentLibrary.getRequestDetailsLatencyScatterChartComponent)

        val template =
          new RequestDetailsPageTemplate(path, requestName, group,
            new StatisticsTextComponent,
            componentLibrary.getRequestDetailsIndicatorChartComponent,
            new ErrorsTableComponent(dataReader.errors(Some(requestName), group)),
            responseTimeDistributionChartComponent,
            responseTimeChartComponent,
            latencyChartComponent,
            requestsChartComponent,
            responsesChartComponent,
            responseTimeScatterChartComponent,
            latencyScatterChartComponent)

        new TemplateWriter(requestFile(reportFolderName, path)).writeToFile(template.getOutput(configuration.core.charset))
      }

    dataReader.statsPaths.foreach {
      case RequestStatsPath(request, group) => generateDetailPage(RequestPath.path(request, group), request, group)
      case _                                =>
    }
  }
}

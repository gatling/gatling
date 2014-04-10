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

import io.gatling.charts.component.{ Component, ComponentLibrary, ErrorTableComponent, StatisticsTextComponent }
import io.gatling.charts.config.ChartsFiles.requestFile
import io.gatling.charts.result.reader.RequestPath
import io.gatling.charts.template.RequestDetailsPageTemplate
import io.gatling.charts.util.Colors._
import io.gatling.core.result._
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.result.reader.DataReader
import io.gatling.core.result.message.Status

class RequestDetailsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

  def generate() {
      def generateDetailPage(path: String, requestName: String, group: Option[Group]) {

          def responseTimeDistributionChartComponent: Component = {
            val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100, Some(requestName), group)
            val okDistributionSeries = new Series("Success", okDistribution, List(BLUE))
            val koDistributionSeries = new Series("Failure", koDistribution, List(RED))

            componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
          }

          def responseTimeChartComponent: Component = {
            val responseTimesSuccessData = dataReader.responseTimeGroupByExecutionStartDate(OK, requestName, group)
            val responseTimesFailuresData = dataReader.responseTimeGroupByExecutionStartDate(KO, requestName, group)
            val responseTimesSuccessSeries = new Series[IntRangeVsTimePlot]("Response Time (success)", responseTimesSuccessData, List(BLUE))
            val responseTimesFailuresSeries = new Series[IntRangeVsTimePlot]("Response Time (failure)", responseTimesFailuresData, List(RED))

            componentLibrary.getRequestDetailsResponseTimeChartComponent(dataReader.runStart, responseTimesSuccessSeries, responseTimesFailuresSeries)
          }

          def requestsChartComponent: Component = {
            val all = dataReader.numberOfRequestsPerSecond(None, Some(requestName), group).sortBy(_.time)
            val oks = dataReader.numberOfRequestsPerSecond(Some(OK), Some(requestName), group).sortBy(_.time)
            val kos = dataReader.numberOfRequestsPerSecond(Some(KO), Some(requestName), group).sortBy(_.time)

            val allSeries = new Series[IntVsTimePlot]("All requests", all, List(BLUE))
            val kosSeries = new Series[IntVsTimePlot]("Failed requests", kos, List(RED))
            val oksSeries = new Series[IntVsTimePlot]("Succeeded requests", oks, List(GREEN))
            val pieRequestsSeries = new Series[PieSlice]("Distribution", PieSlice("Success", count(oks)) :: PieSlice("Failures", count(kos)) :: Nil, List(GREEN, RED))

            componentLibrary.getRequestsChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
          }

          def responsesChartComponent: Component = {
            val all = dataReader.numberOfResponsesPerSecond(None, Some(requestName), group).sortBy(_.time)
            val oks = dataReader.numberOfResponsesPerSecond(Some(OK), Some(requestName), group).sortBy(_.time)
            val kos = dataReader.numberOfResponsesPerSecond(Some(KO), Some(requestName), group).sortBy(_.time)

            val allSeries = new Series[IntVsTimePlot]("All responses", all, List(BLUE))
            val kosSeries = new Series[IntVsTimePlot]("Failed responses", kos, List(RED))
            val oksSeries = new Series[IntVsTimePlot]("Succeeded responses", oks, List(GREEN))
            val pieRequestsSeries = new Series[PieSlice]("Distribution", PieSlice("Success", count(oks)) :: PieSlice("Failures", count(kos)) :: Nil, List(GREEN, RED))

            componentLibrary.getResponsesChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
          }

          def latencyChartComponent: Component = {
            val latencySuccessData = dataReader.latencyGroupByExecutionStartDate(OK, requestName, group)
            val latencyFailuresData = dataReader.latencyGroupByExecutionStartDate(KO, requestName, group)

            val latencySuccessSeries = new Series[IntRangeVsTimePlot]("Latency (success)", latencySuccessData, List(BLUE))
            val latencyFailuresSeries = new Series[IntRangeVsTimePlot]("Latency (failure)", latencyFailuresData, List(RED))

            componentLibrary.getRequestDetailsLatencyChartComponent(dataReader.runStart, latencySuccessSeries, latencyFailuresSeries)
          }

          def scatterChartComponent(datasource: (Status, String, Option[Group]) => Seq[IntVsTimePlot],
                                    componentFactory: (Series[IntVsTimePlot], Series[IntVsTimePlot]) => Component): Component = {

            val scatterPlotSuccessData = datasource(OK, requestName, group)
            val scatterPlotFailuresData = datasource(KO, requestName, group)
            val scatterPlotSuccessSeries = new Series[IntVsTimePlot]("Successes", scatterPlotSuccessData, List(TRANSLUCID_BLUE))
            val scatterPlotFailuresSeries = new Series[IntVsTimePlot]("Failures", scatterPlotFailuresData, List(TRANSLUCID_RED))

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
            new ErrorTableComponent(dataReader.errors(Some(requestName), group)),
            responseTimeDistributionChartComponent,
            responseTimeChartComponent,
            latencyChartComponent,
            requestsChartComponent,
            responsesChartComponent,
            responseTimeScatterChartComponent,
            latencyScatterChartComponent)

        new TemplateWriter(requestFile(runOn, path)).writeToFile(template.getOutput)
      }

    dataReader.statsPaths.foreach {
      case RequestStatsPath(request, group) => generateDetailPage(RequestPath.path(request, group), request, group)
      case _                                =>
    }
  }
}

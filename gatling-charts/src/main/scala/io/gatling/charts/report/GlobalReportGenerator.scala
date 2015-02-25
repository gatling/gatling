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
import io.gatling.charts.config.ChartsFiles.globalFile
import io.gatling.charts.template.GlobalPageTemplate
import io.gatling.charts.util.Colors._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result._
import io.gatling.core.result.message.{ KO, OK }

private[charts] class GlobalReportGenerator(reportsGenerationInputs: ReportsGenerationInputs, componentLibrary: ComponentLibrary)(implicit configuration: GatlingConfiguration)
    extends ReportGenerator {

  def generate(): Unit = {
    import reportsGenerationInputs._

      def activeSessionsChartComponent = {

        val baseColors = List(Blue, Green, Red, Yellow, Cyan, Lime, Purple, Pink, LightBlue, LightOrange, LightRed, LightLime, LightPurple, LightPink)
        val seriesColors = Iterator.continually(baseColors).flatten.take(dataReader.scenarioNames.size).toList

        val activeSessionsSeries: Seq[Series[IntVsTimePlot]] = dataReader
          .scenarioNames
          .map { scenarioName => scenarioName -> dataReader.numberOfActiveSessionsPerSecond(Some(scenarioName)) }
          .reverse
          .zip(seriesColors)
          .map { case ((scenarioName, data), color) => new Series[IntVsTimePlot](scenarioName, data, List(color)) }

        componentLibrary.getActiveSessionsChartComponent(dataReader.runStart, activeSessionsSeries)
      }

      def requestsChartComponent: Component = {
        val globalCounts = dataReader.numberOfRequestsPerSecond().sortBy(_.time)

        val globalCountsSeries = new Series[CountsVsTimePlot]("Number of Requests per sec", globalCounts, List(Blue, Red, Green))
        val pieRequestsSeries = new Series[PieSlice](Series.Distribution, PieSlice(Series.OK, count(globalCounts, OK)) :: PieSlice(Series.KO, count(globalCounts, KO)) :: Nil, List(Green, Red))

        componentLibrary.getRequestsChartComponent(dataReader.runStart, globalCountsSeries, pieRequestsSeries)
      }

      def responsesChartComponent: Component = {
        val globalCounts = dataReader.numberOfResponsesPerSecond().sortBy(_.time)

        val globalCountsSeries = new Series[CountsVsTimePlot]("Number of Responses per sec", globalCounts, List(Blue, Red, Green))
        val pieRequestsSeries = new Series[PieSlice](Series.Distribution, PieSlice(Series.OK, count(globalCounts, OK)) :: PieSlice(Series.KO, count(globalCounts, KO)) :: Nil, List(Green, Red))

        componentLibrary.getResponsesChartComponent(dataReader.runStart, globalCountsSeries, pieRequestsSeries)
      }

      def responseTimeDistributionChartComponent: Component = {
        val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100)
        val okDistributionSeries = new Series(Series.OK, okDistribution, List(Blue))
        val koDistributionSeries = new Series(Series.KO, koDistribution, List(Red))

        componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
      }

      def responseTimeChartComponent: Component = {
        val responseTimesPercentilesSuccessData = dataReader.responseTimePercentilesOverTime(OK, None, None)

        val responseTimesSuccessSeries = new Series[PercentilesVsTimePlot](s"Response Time Percentiles over Time (${Series.OK})", responseTimesPercentilesSuccessData, ReportGenerator.PercentilesColors)

        componentLibrary.getRequestDetailsResponseTimeChartComponent(dataReader.runStart, responseTimesSuccessSeries)
      }

      def latencyChartComponent: Component = {

        val latencyPercentilesSuccessData = dataReader.latencyPercentilesOverTime(OK, None, None)

        val latencySuccessSeries = new Series[PercentilesVsTimePlot](s"Latency Percentiles over Time (${Series.OK})", latencyPercentilesSuccessData, ReportGenerator.PercentilesColors)

        componentLibrary.getRequestDetailsLatencyChartComponent(dataReader.runStart, latencySuccessSeries)
      }

    val template = new GlobalPageTemplate(
      componentLibrary.getNumberOfRequestsChartComponent(dataReader.requestNames.size),
      componentLibrary.getRequestDetailsIndicatorChartComponent,
      new AssertionsTableComponent(assertionResults),
      new StatisticsTableComponent,
      new ErrorsTableComponent(dataReader.errors(None, None)),
      activeSessionsChartComponent,
      responseTimeDistributionChartComponent,
      responseTimeChartComponent,
      latencyChartComponent,
      requestsChartComponent,
      responsesChartComponent)

    new TemplateWriter(globalFile(reportFolderName)).writeToFile(template.getOutput(configuration.core.charset))
  }
}

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
        val all = dataReader.numberOfRequestsPerSecond().sortBy(_.time)
        val oks = dataReader.numberOfRequestsPerSecond(Some(OK)).sortBy(_.time)
        val kos = dataReader.numberOfRequestsPerSecond(Some(KO)).sortBy(_.time)

        val allSeries = new Series[IntVsTimePlot](Series.All, all, List(Blue))
        val kosSeries = new Series[IntVsTimePlot](Series.KO, kos, List(Red))
        val oksSeries = new Series[IntVsTimePlot](Series.OK, oks, List(Green))
        val pieRequestsSeries = new Series[PieSlice](Series.Distribution, PieSlice(Series.OK, count(oks)) :: PieSlice(Series.KO, count(kos)) :: Nil, List(Green, Red))

        componentLibrary.getRequestsChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
      }

      def responsesChartComponent: Component = {
        val all = dataReader.numberOfResponsesPerSecond().sortBy(_.time)
        val oks = dataReader.numberOfResponsesPerSecond(Some(OK)).sortBy(_.time)
        val kos = dataReader.numberOfResponsesPerSecond(Some(KO)).sortBy(_.time)

        val allSeries = new Series[IntVsTimePlot](Series.All, all, List(Blue))
        val kosSeries = new Series[IntVsTimePlot](Series.KO, kos, List(Red))
        val oksSeries = new Series[IntVsTimePlot](Series.OK, oks, List(Green))
        val pieRequestsSeries = new Series[PieSlice](Series.Distribution, PieSlice(Series.OK, count(oks)) :: PieSlice(Series.KO, count(kos)) :: Nil, List(Green, Red))

        componentLibrary.getResponsesChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
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

    val template = new GlobalPageTemplate(
      componentLibrary.getNumberOfRequestsChartComponent(dataReader.requestNames.size),
      componentLibrary.getRequestDetailsIndicatorChartComponent,
      new AssertionsTableComponent(assertionResults),
      new StatisticsTableComponent,
      new ErrorsTableComponent(dataReader.errors(None, None)),
      activeSessionsChartComponent,
      responseTimeDistributionChartComponent,
      responseTimeChartComponent,
      requestsChartComponent,
      responsesChartComponent)

    new TemplateWriter(globalFile(reportFolderName)).writeToFile(template.getOutput(configuration.core.charset))
  }
}

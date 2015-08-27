/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import io.gatling.charts.config.ChartsFiles.globalFile
import io.gatling.charts.template.GlobalPageTemplate
import io.gatling.charts.util.Colors._
import io.gatling.commons.stats.{ Group, KO, OK, Status }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats._

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

      def responseTimeDistributionChartComponent: Component = {
        val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100, None, None)
        val okDistributionSeries = new Series(Series.OK, okDistribution, List(Blue))
        val koDistributionSeries = new Series(Series.KO, koDistribution, List(Red))

        componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
      }

      def responseTimeChartComponent: Component =
        percentilesChartComponent(dataReader.responseTimePercentilesOverTime, componentLibrary.getRequestDetailsResponseTimeChartComponent, "Response Time Percentiles over Time")

      def percentilesChartComponent(
        dataSource:       (Status, Option[String], Option[Group]) => Iterable[PercentilesVsTimePlot],
        componentFactory: (Long, Series[PercentilesVsTimePlot]) => Component,
        title:            String
      ): Component = {
        val successData = dataSource(OK, None, None)
        val successSeries = new Series[PercentilesVsTimePlot](s"$title (${Series.OK})", successData, ReportGenerator.PercentilesColors)

        componentFactory(dataReader.runStart, successSeries)
      }

      def requestsChartComponent: Component =
        countsChartComponent(dataReader.numberOfRequestsPerSecond, componentLibrary.getRequestsChartComponent)

      def responsesChartComponent: Component =
        countsChartComponent(dataReader.numberOfResponsesPerSecond, componentLibrary.getResponsesChartComponent)

      def countsChartComponent(
        dataSource:       (Option[String], Option[Group]) => Seq[CountsVsTimePlot],
        componentFactory: (Long, Series[CountsVsTimePlot], Series[PieSlice]) => Component
      ): Component = {
        val counts = dataSource(None, None).sortBy(_.time)

        val countsSeries = new Series[CountsVsTimePlot]("", counts, List(Blue, Red, Green))
        val okPieSlice = PieSlice(Series.OK, count(counts, OK))
        val koPieSlice = PieSlice(Series.KO, count(counts, KO))
        val pieRequestsSeries = new Series[PieSlice](Series.Distribution, Seq(okPieSlice, koPieSlice), List(Green, Red))

        componentFactory(dataReader.runStart, countsSeries, pieRequestsSeries)
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
      responsesChartComponent
    )

    new TemplateWriter(globalFile(reportFolderName)).writeToFile(template.getOutput(configuration.core.charset))
  }
}

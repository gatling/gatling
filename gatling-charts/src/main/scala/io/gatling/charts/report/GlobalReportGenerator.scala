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

import io.gatling.charts.component.{ Component, ComponentLibrary, ErrorTableComponent, StatisticsTableComponent }
import io.gatling.charts.config.ChartsFiles.globalFile
import io.gatling.charts.template.GlobalPageTemplate
import io.gatling.charts.util.Colors._
import io.gatling.core.result._
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.result.reader.DataReader

class GlobalReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

  def generate() {
      def activeSessionsChartComponent = {
        val activeSessionsSeries: Seq[Series[IntVsTimePlot]] = dataReader
          .scenarioNames
          .map { scenarioName => scenarioName -> dataReader.numberOfActiveSessionsPerSecond(Some(scenarioName)) }
          .reverse
          .zip(List(Blue, Green, Red, Yellow, Cyan, Lime, Purple, Pink, LightBlue, LightOrange, LightRed, LightLime, LightPurple, LightPink))
          .map { case ((scenarioName, data), color) => new Series[IntVsTimePlot](scenarioName, data, List(color)) }

        componentLibrary.getActiveSessionsChartComponent(dataReader.runStart, activeSessionsSeries)
      }

      def requestsChartComponent: Component = {
        val all = dataReader.numberOfRequestsPerSecond().sortBy(_.time)
        val oks = dataReader.numberOfRequestsPerSecond(Some(OK)).sortBy(_.time)
        val kos = dataReader.numberOfRequestsPerSecond(Some(KO)).sortBy(_.time)

        val allSeries = new Series[IntVsTimePlot]("All requests", all, List(Blue))
        val kosSeries = new Series[IntVsTimePlot]("Failed requests", kos, List(Red))
        val oksSeries = new Series[IntVsTimePlot]("Succeeded requests", oks, List(Green))
        val pieRequestsSeries = new Series[PieSlice]("Distribution", PieSlice("Success", count(oks)) :: PieSlice("Failures", count(kos)) :: Nil, List(Green, Red))

        componentLibrary.getRequestsChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
      }

      def responsesChartComponent: Component = {
        val all = dataReader.numberOfResponsesPerSecond().sortBy(_.time)
        val oks = dataReader.numberOfResponsesPerSecond(Some(OK)).sortBy(_.time)
        val kos = dataReader.numberOfResponsesPerSecond(Some(KO)).sortBy(_.time)

        val allSeries = new Series[IntVsTimePlot]("All responses", all, List(Blue))
        val kosSeries = new Series[IntVsTimePlot]("Failed responses", kos, List(Red))
        val oksSeries = new Series[IntVsTimePlot]("Succeeded responses", oks, List(Green))
        val pieRequestsSeries = new Series[PieSlice]("Distribution", PieSlice("Success", count(oks)) :: PieSlice("Failures", count(kos)) :: Nil, List(Green, Red))

        componentLibrary.getResponsesChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
      }

      def responseTimeDistributionChartComponent: Component = {
        val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100)
        val okDistributionSeries = new Series("Success", okDistribution, List(Blue))
        val koDistributionSeries = new Series("Failure", koDistribution, List(Red))

        componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
      }

      def responseTimeChartComponent: Component = {
        val responseTimesPercentilesSuccessData = dataReader.responseTimePercentilesOverTime(OK, None, None)

        val responseTimesSuccessSeries = new Series[PercentilesVsTimePlot]("Response Time Percentiles over Time (success)", responseTimesPercentilesSuccessData, ReportGenerator.PercentilesColors)

        componentLibrary.getRequestDetailsResponseTimeChartComponent(dataReader.runStart, responseTimesSuccessSeries)
      }

    val template = new GlobalPageTemplate(
      componentLibrary.getNumberOfRequestsChartComponent(dataReader.requestNames.size),
      componentLibrary.getRequestDetailsIndicatorChartComponent,
      new StatisticsTableComponent,
      new ErrorTableComponent(dataReader.errors(None, None)),
      activeSessionsChartComponent,
      responseTimeDistributionChartComponent,
      responseTimeChartComponent,
      requestsChartComponent,
      responsesChartComponent)

    new TemplateWriter(globalFile(runOn)).writeToFile(template.getOutput)
  }
}

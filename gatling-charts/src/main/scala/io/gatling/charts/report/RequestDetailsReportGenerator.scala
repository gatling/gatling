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
import io.gatling.commons.stats.{ KO, OK }
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

        componentLibrary.getDistributionComponent(
          "responseTimeDistributionContainerId",
          "Response Time",
          "Requests",
          okDistribution,
          koDistribution
        )
      }

      def responseTimeChartComponent: Component = {
        val successData = logFileData.responseTimePercentilesOverTime(OK, Some(requestContainer.name), requestContainer.group)

        componentLibrary.getPercentilesOverTimeComponent(
          "responseTimeOverTimeContainerId",
          s"Response Time Percentiles over Time (${Series.OK})",
          "Response Time",
          logFileData.runInfo.injectStart,
          successData
        )
      }

      def requestsChartComponent: Component =
        componentLibrary.getRequestsComponent(
          "requestsContainerId",
          logFileData.runInfo.injectStart,
          logFileData.numberOfRequestsPerSecond(Some(requestContainer.name), requestContainer.group).sortBy(_.time)
        )

      def responsesChartComponent: Component =
        componentLibrary.getResponsesComponent(
          "responsesContainerId",
          logFileData.runInfo.injectStart,
          logFileData.numberOfResponsesPerSecond(Some(requestContainer.name), requestContainer.group).sortBy(_.time)
        )

      def responseTimeScatterChartComponent: Component =
        componentLibrary.getResponseTimeScatterComponent(
          "responseTimeScatterContainerId",
          logFileData.responseTimeAgainstGlobalNumberOfRequestsPerSec(OK, requestContainer.name, requestContainer.group),
          logFileData.responseTimeAgainstGlobalNumberOfRequestsPerSec(KO, requestContainer.name, requestContainer.group)
        )

      val ranges = logFileData.numberOfRequestInResponseTimeRanges(Some(requestContainer.name), requestContainer.group)

      val path = RequestPath.path(requestContainer.name, requestContainer.group)

      val template =
        new DetailsPageTemplate(
          logFileData.runInfo,
          path,
          requestContainer,
          rootContainer,
          new SchemaContainerComponent(
            componentLibrary.getRangesComponent("RangesContainerId", "Response Time Ranges", "requests", ranges, large = true),
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

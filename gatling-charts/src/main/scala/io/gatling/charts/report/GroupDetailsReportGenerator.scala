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

import io.gatling.charts.component.{ Component, ComponentLibrary, DetailsStatsTableComponent, ErrorsTableComponent }
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.stats.{ LogFileData, RequestPath }
import io.gatling.charts.template.DetailsPageTemplate
import io.gatling.commons.stats.OK
import io.gatling.core.config.ReportsConfiguration

private[charts] class GroupDetailsReportGenerator(
    logFileData: LogFileData,
    rootContainer: GroupContainer,
    chartsFiles: ChartsFiles,
    componentLibrary: ComponentLibrary,
    charset: Charset,
    configuration: ReportsConfiguration
) extends ReportGenerator {
  def generate(): Unit = {
    def generateDetailPage(groupContainer: GroupContainer): Unit = {
      def cumulatedResponseTimeChartComponent: Component =
        componentLibrary.getPercentilesOverTimeComponent(
          "Group Cumulated Response Time Percentiles over Time (OK)",
          "Cumulated Response Time",
          logFileData.runInfo.injectStart,
          logFileData.groupCumulatedResponseTimePercentilesOverTime(OK, groupContainer.group)
        )

      def cumulatedResponseTimeDistributionChartComponent: Component = {
        val (distributionSuccess, distributionFailure) = logFileData.groupCumulatedResponseTimeDistribution(100, groupContainer.group)

        componentLibrary.getDistributionComponent(
          "Group Cumulated Response Time",
          "Groups",
          distributionSuccess,
          distributionFailure
        )
      }

      def durationChartComponent: Component =
        componentLibrary.getPercentilesOverTimeComponent(
          "Group Duration Percentiles over Time (OK)",
          "Duration",
          logFileData.runInfo.injectStart,
          logFileData.groupDurationPercentilesOverTime(OK, groupContainer.group)
        )

      def durationDistributionChartComponent: Component = {
        val (distributionSuccess, distributionFailure) = logFileData.groupDurationDistribution(100, groupContainer.group)

        componentLibrary.getDistributionComponent(
          "Group Duration",
          "Groups",
          distributionSuccess,
          distributionFailure
        )
      }

      val ranges = logFileData.numberOfRequestInResponseTimeRanges(None, Some(groupContainer.group))

      val path = RequestPath.path(groupContainer.group)

      val template = new DetailsPageTemplate(
        logFileData.runInfo,
        path,
        groupContainer,
        rootContainer,
        new SchemaContainerComponent(
          componentLibrary.getRangesComponent("Group Duration Ranges", "groups", ranges, large = true),
          new DetailsStatsTableComponent(groupContainer.stats, configuration.indicators)
        ),
        new ErrorsTableComponent(logFileData.errors(None, Some(groupContainer.group))),
        durationDistributionChartComponent,
        durationChartComponent,
        cumulatedResponseTimeDistributionChartComponent,
        cumulatedResponseTimeChartComponent
      )

      new TemplateWriter(chartsFiles.groupFile(path)).writeToFile(template.getOutput, charset)
    }

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def generateDetailPageRec(groupContainers: Iterable[GroupContainer]): Unit =
      groupContainers.foreach { groupContainer =>
        generateDetailPage(groupContainer)
        generateDetailPageRec(groupContainer.groups.values)
      }

    generateDetailPageRec(rootContainer.groups.values)
  }
}

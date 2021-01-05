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

import io.gatling.charts.component.{ Component, ComponentLibrary, ErrorsTableComponent, StatisticsTextComponent }
import io.gatling.charts.config.ChartsFiles
import io.gatling.charts.stats.{ PercentilesVsTimePlot, RequestPath, Series }
import io.gatling.charts.template.GroupDetailsPageTemplate
import io.gatling.charts.util.Colors._
import io.gatling.commons.shared.unstable.model.stats.{ Group, GroupStatsPath }
import io.gatling.commons.stats.OK
import io.gatling.core.config.GatlingConfiguration

private[charts] class GroupDetailsReportGenerator(
    reportsGenerationInputs: ReportsGenerationInputs,
    chartsFiles: ChartsFiles,
    componentLibrary: ComponentLibrary
)(implicit
    configuration: GatlingConfiguration
) extends ReportGenerator {

  def generate(): Unit = {
    import reportsGenerationInputs._

    def generateDetailPage(path: String, group: Group): Unit = {
      def cumulatedResponseTimeChartComponent: Component = {
        val dataSuccess = logFileReader.groupCumulatedResponseTimePercentilesOverTime(OK, group)
        val seriesSuccess =
          new Series[PercentilesVsTimePlot]("Group Cumulated Response Time Percentiles over Time (success)", dataSuccess, ReportGenerator.PercentilesColors)

        componentLibrary.getGroupDetailsDurationChartComponent(
          "cumulatedResponseTimeChartContainer",
          "Cumulated Response Time (ms)",
          logFileReader.runStart,
          seriesSuccess
        )
      }

      def cumulatedResponseTimeDistributionChartComponent: Component = {
        val (distributionSuccess, distributionFailure) = logFileReader.groupCumulatedResponseTimeDistribution(100, group)
        val distributionSeriesSuccess = new Series("Group cumulated response time (success)", distributionSuccess, List(Blue))
        val distributionSeriesFailure = new Series("Group cumulated response time (failure)", distributionFailure, List(Red))

        componentLibrary.getGroupDetailsDurationDistributionChartComponent(
          "Group Cumulated Response Time Distribution",
          "cumulatedResponseTimeDistributionContainer",
          distributionSeriesSuccess,
          distributionSeriesFailure
        )
      }

      def durationChartComponent: Component = {
        val dataSuccess = logFileReader.groupDurationPercentilesOverTime(OK, group)
        val seriesSuccess = new Series[PercentilesVsTimePlot]("Group Duration Percentiles over Time (success)", dataSuccess, ReportGenerator.PercentilesColors)

        componentLibrary.getGroupDetailsDurationChartComponent("durationContainer", "Duration (ms)", logFileReader.runStart, seriesSuccess)
      }

      def durationDistributionChartComponent: Component = {
        val (distributionSuccess, distributionFailure) = logFileReader.groupDurationDistribution(100, group)
        val distributionSeriesSuccess = new Series("Group duration (success)", distributionSuccess, List(Blue))
        val distributionSeriesFailure = new Series("Group duration (failure)", distributionFailure, List(Red))

        componentLibrary.getGroupDetailsDurationDistributionChartComponent(
          "Group Duration Distribution",
          "durationDistributionContainer",
          distributionSeriesSuccess,
          distributionSeriesFailure
        )
      }

      def statisticsComponent: Component = new StatisticsTextComponent

      def indicatorChartComponent: Component = componentLibrary.getRequestDetailsIndicatorChartComponent

      val template = new GroupDetailsPageTemplate(
        group,
        statisticsComponent,
        indicatorChartComponent,
        new ErrorsTableComponent(logFileReader.errors(None, Some(group))),
        cumulatedResponseTimeChartComponent,
        cumulatedResponseTimeDistributionChartComponent,
        durationChartComponent,
        durationDistributionChartComponent
      )

      new TemplateWriter(chartsFiles.groupFile(path)).writeToFile(template.getOutput(configuration.core.charset))
    }

    logFileReader.statsPaths.foreach {
      case GroupStatsPath(group) => generateDetailPage(RequestPath.path(group), group)
      case _                     =>
    }
  }
}

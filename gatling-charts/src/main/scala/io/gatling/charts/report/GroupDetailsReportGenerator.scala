/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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
import io.gatling.charts.util.Color
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
        val dataSuccess = logFileData.groupCumulatedResponseTimePercentilesOverTime(OK, group)
        val seriesSuccess =
          new Series[PercentilesVsTimePlot]("Group Cumulated Response Time Percentiles over Time (success)", dataSuccess, Color.Requests.Percentiles)

        componentLibrary.getGroupDetailsDurationChartComponent(
          "cumulatedResponseTimeChartContainer",
          "Cumulated Response Time (ms)",
          logFileData.runInfo.injectStart,
          seriesSuccess
        )
      }

      def cumulatedResponseTimeDistributionChartComponent: Component = {
        val (distributionSuccess, distributionFailure) = logFileData.groupCumulatedResponseTimeDistribution(100, group)
        val distributionSeriesSuccess = new Series(Series.OK, distributionSuccess, List(Color.Requests.Ok))
        val distributionSeriesFailure = new Series(Series.KO, distributionFailure, List(Color.Requests.Ko))

        componentLibrary.getGroupDetailsDurationDistributionChartComponent(
          "Group Cumulated Response Time Distribution",
          "cumulatedResponseTimeDistributionContainer",
          distributionSeriesSuccess,
          distributionSeriesFailure
        )
      }

      def durationChartComponent: Component = {
        val dataSuccess = logFileData.groupDurationPercentilesOverTime(OK, group)
        val seriesSuccess = new Series[PercentilesVsTimePlot]("Group Duration Percentiles over Time (success)", dataSuccess, Color.Requests.Percentiles)

        componentLibrary.getGroupDetailsDurationChartComponent("durationContainer", "Duration (ms)", logFileData.runInfo.injectStart, seriesSuccess)
      }

      def durationDistributionChartComponent: Component = {
        val (distributionSuccess, distributionFailure) = logFileData.groupDurationDistribution(100, group)
        val distributionSeriesSuccess = new Series(Series.OK, distributionSuccess, List(Color.Requests.Ok))
        val distributionSeriesFailure = new Series(Series.KO, distributionFailure, List(Color.Requests.Ko))

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
        logFileData.runInfo,
        group,
        new SchemaContainerComponent(
          statisticsComponent,
          indicatorChartComponent
        ),
        new ErrorsTableComponent(logFileData.errors(None, Some(group))),
        cumulatedResponseTimeChartComponent,
        cumulatedResponseTimeDistributionChartComponent,
        durationChartComponent,
        durationDistributionChartComponent
      )

      new TemplateWriter(chartsFiles.groupFile(path)).writeToFile(template.getOutput(configuration.core.charset))
    }

    logFileData.statsPaths.foreach {
      case GroupStatsPath(group) => generateDetailPage(RequestPath.path(group), group)
      case _                     =>
    }
  }
}

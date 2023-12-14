/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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
import io.gatling.charts.stats.{ Group, GroupStatsPath, PercentilesVsTimePlot, RequestPath, Series }
import io.gatling.charts.template.GroupDetailsPageTemplate
import io.gatling.charts.util.Color
import io.gatling.commons.stats.OK
import io.gatling.core.config.ChartingConfiguration

private[charts] class GroupDetailsReportGenerator(
    reportsGenerationInputs: ReportsGenerationInputs,
    chartsFiles: ChartsFiles,
    componentLibrary: ComponentLibrary,
    charset: Charset,
    configuration: ChartingConfiguration
) extends ReportGenerator {
  def generate(): Unit = {
    import reportsGenerationInputs._

    def generateDetailPage(path: String, group: Group): Unit = {
      def cumulatedResponseTimeChartComponent: Component = {
        val dataSuccess = logFileData.groupCumulatedResponseTimePercentilesOverTime(OK, group)
        val seriesSuccess =
          new Series[PercentilesVsTimePlot]("Group Cumulated Response Time Percentiles over Time (OK)", dataSuccess, Color.Requests.Percentiles)

        componentLibrary.getPercentilesOverTimeComponent(
          "Cumulated Response Time",
          logFileData.runInfo.injectStart,
          seriesSuccess
        )
      }

      def cumulatedResponseTimeDistributionChartComponent: Component = {
        val (distributionSuccess, distributionFailure) = logFileData.groupCumulatedResponseTimeDistribution(100, group)
        val distributionSeriesSuccess = new Series(Series.OK, distributionSuccess, List(Color.Requests.Ok))
        val distributionSeriesFailure = new Series(Series.KO, distributionFailure, List(Color.Requests.Ko))

        componentLibrary.getDistributionComponent(
          "Group Cumulated Response Time",
          "Groups",
          distributionSeriesSuccess,
          distributionSeriesFailure
        )
      }

      def durationChartComponent: Component = {
        val dataSuccess = logFileData.groupDurationPercentilesOverTime(OK, group)
        val seriesSuccess = new Series[PercentilesVsTimePlot]("Group Duration Percentiles over Time (OK)", dataSuccess, Color.Requests.Percentiles)

        componentLibrary.getPercentilesOverTimeComponent("Duration", logFileData.runInfo.injectStart, seriesSuccess)
      }

      def durationDistributionChartComponent: Component = {
        val (distributionSuccess, distributionFailure) = logFileData.groupDurationDistribution(100, group)
        val distributionSeriesSuccess = new Series(Series.OK, distributionSuccess, List(Color.Requests.Ok))
        val distributionSeriesFailure = new Series(Series.KO, distributionFailure, List(Color.Requests.Ko))

        componentLibrary.getDistributionComponent(
          "Group Duration",
          "Groups",
          distributionSeriesSuccess,
          distributionSeriesFailure
        )
      }

      val template = new GroupDetailsPageTemplate(
        logFileData.runInfo,
        group,
        new SchemaContainerComponent(
          componentLibrary.getRangesComponent("Group Duration Ranges", "groups", large = true),
          new DetailsStatsTableComponent(configuration.indicators)
        ),
        new ErrorsTableComponent(logFileData.errors(None, Some(group))),
        durationDistributionChartComponent,
        durationChartComponent,
        cumulatedResponseTimeDistributionChartComponent,
        cumulatedResponseTimeChartComponent
      )

      new TemplateWriter(chartsFiles.groupFile(path)).writeToFile(template.getOutput, charset)
    }

    logFileData.statsPaths.foreach {
      case GroupStatsPath(group) => generateDetailPage(RequestPath.path(group), group)
      case _                     =>
    }
  }
}

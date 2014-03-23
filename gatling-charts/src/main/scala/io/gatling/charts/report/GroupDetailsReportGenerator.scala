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
import io.gatling.charts.template.GroupDetailsPageTemplate
import io.gatling.charts.util.Colors.{ BLUE, RED }
import io.gatling.core.result.{ Group, GroupStatsPath, IntRangeVsTimePlot, IntVsTimePlot, Series }
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.result.reader.DataReader

class GroupDetailsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

  def generate() {
      def generateDetailPage(group: Group) {
          def cumulatedResponseTimeChartComponent: Component = {
            val dataSuccess = dataReader.groupCumulatedResponseTimeGroupByExecutionStartDate(OK, group)
            val dataFailure = dataReader.groupCumulatedResponseTimeGroupByExecutionStartDate(KO, group)
            val seriesSuccess = new Series[IntRangeVsTimePlot]("Group cumulated response time (success)", dataSuccess, List(BLUE))
            val seriesFailure = new Series[IntRangeVsTimePlot]("Group cumulated response time (failure)", dataFailure, List(RED))

            componentLibrary.getGroupDurationChartComponent("Group Cumulated Response Time during Simulation", "cumulatedResponseTimeChartContainer", "Cumulated Response Time (ms)", dataReader.runStart, seriesSuccess, seriesFailure)
          }

          def cumulatedResponseTimeDistributionChartComponent: Component = {
            val (distributionSuccess, distributionFailure) = dataReader.groupCumulatedResponseTimeDistribution(100, group)
            val distributionSeriesSuccess = new Series[IntVsTimePlot]("Group cumulated response time (success)", distributionSuccess, List(BLUE))
            val distributionSeriesFailure = new Series[IntVsTimePlot]("Group cumulated response time (failure)", distributionFailure, List(RED))

            componentLibrary.getGroupDetailsDurationDistributionChartComponent("Group Cumulated Response Time Distribution", "cumulatedResponseTimeDistributionContainer", distributionSeriesSuccess, distributionSeriesFailure)
          }

          def durationChartComponent: Component = {
            val dataSuccess = dataReader.groupDurationGroupByExecutionStartDate(OK, group)
            val dataFailure = dataReader.groupDurationGroupByExecutionStartDate(KO, group)
            val seriesSuccess = new Series[IntRangeVsTimePlot]("Group duration (success)", dataSuccess, List(BLUE))
            val seriesFailure = new Series[IntRangeVsTimePlot]("Group duration (failure)", dataFailure, List(RED))

            componentLibrary.getGroupDurationChartComponent("Group Duration during Simulation", "durationContainer", "Duration (ms)", dataReader.runStart, seriesSuccess, seriesFailure)
          }

          def durationDistributionChartComponent: Component = {
            val (distributionSuccess, distributionFailure) = dataReader.groupDurationDistribution(100, group)
            val distributionSeriesSuccess = new Series[IntVsTimePlot]("Group duration (success)", distributionSuccess, List(BLUE))
            val distributionSeriesFailure = new Series[IntVsTimePlot]("Group duration (failure)", distributionFailure, List(RED))

            componentLibrary.getGroupDetailsDurationDistributionChartComponent("Group Duration Distribution", "durationDistributionContainer", distributionSeriesSuccess, distributionSeriesFailure)
          }

          def statisticsComponent: Component = new StatisticsTextComponent

          def indicatorChartComponent: Component = componentLibrary.getRequestDetailsIndicatorChartComponent

        val template = new GroupDetailsPageTemplate(
          group,
          statisticsComponent,
          indicatorChartComponent,
          new ErrorTableComponent(dataReader.errors(None, Some(group))),
          cumulatedResponseTimeChartComponent,
          cumulatedResponseTimeDistributionChartComponent,
          durationChartComponent,
          durationDistributionChartComponent)

        new TemplateWriter(requestFile(runOn, group.name)).writeToFile(template.getOutput)
      }

    dataReader.statsPaths.foreach {
      case GroupStatsPath(group) => generateDetailPage(group)
      case _                     =>
    }
  }
}

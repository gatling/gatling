/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import io.gatling.charts.component.{ Component, ComponentLibrary, StatisticsTextComponent }
import io.gatling.charts.config.ChartsFiles.requestFile
import io.gatling.charts.template.GroupDetailsPageTemplate
import io.gatling.charts.util.Colors.{ BLUE, RED }
import io.gatling.core.result.{ Group, GroupStatsPath, IntRangeVsTimePlot, IntVsTimePlot, Series }
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.result.reader.DataReader

class GroupDetailsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		def generateDetailPage(group: Group) {
			def responseTimeChartComponent: Component = {
				val durationsDataSuccess = dataReader.responseTimeGroupByExecutionStartDate(OK, None, Some(group))
				val durationsDataFailure = dataReader.responseTimeGroupByExecutionStartDate(KO, None, Some(group))
				val durationsSeriesSuccess = new Series[IntRangeVsTimePlot]("Group duration (success)", durationsDataSuccess, List(BLUE))
				val durationsSeriesFailure = new Series[IntRangeVsTimePlot]("Group duration (failure)", durationsDataFailure, List(RED))

				componentLibrary.getGroupDurationChartComponent(dataReader.runStart, durationsSeriesSuccess, durationsSeriesFailure)
			}

			def responseTimeDistributionChartComponent: Component = {
				val (distributionSuccess, distributionFailure) = dataReader.responseTimeDistribution(100, None, Some(group))
				val distributionSeriesSuccess = new Series[IntVsTimePlot]("Group duration (failure)", distributionSuccess, List(BLUE))
				val distributionSeriesFailure = new Series[IntVsTimePlot]("Group duration (failure)", distributionFailure, List(RED))

				componentLibrary.getGroupDetailsDurationDistributionChartComponent(distributionSeriesSuccess, distributionSeriesFailure)
			}

			def statisticsComponent: Component = new StatisticsTextComponent

			def indicatorChartComponent: Component = componentLibrary.getRequestDetailsIndicatorChartComponent

			val template =
				new GroupDetailsPageTemplate(group,
					responseTimeChartComponent,
					responseTimeDistributionChartComponent,
					statisticsComponent,
					indicatorChartComponent)

			new TemplateWriter(requestFile(runOn, group.name)).writeToFile(template.getOutput)
		}

		dataReader.statsPaths.foreach {
			case GroupStatsPath(group) => generateDetailPage(group)
			case _ =>
		}
	}
}

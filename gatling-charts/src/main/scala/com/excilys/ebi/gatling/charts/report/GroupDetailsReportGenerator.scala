/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.charts.report

import com.excilys.ebi.gatling.charts.component.{ Component, ComponentLibrary, StatisticsTextComponent }
import com.excilys.ebi.gatling.charts.config.ChartsFiles.requestFile
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.GroupDetailsPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ BLUE, RED, TRANSLUCID_BLUE, TRANSLUCID_RED }
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ KO, OK }
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.result.{ Group, RequestPath }

class GroupDetailsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		def generateDetailPage(group: Group) {
			def responseTimeChartComponent: Component = {
				val durationsData = dataReader.responseTimeGroupByExecutionStartDate(OK, None, Some(group))
				val durationsSeries = new Series[Int, (Int, Int)]("Group duration", durationsData, List(BLUE))

				componentLibrary.getGroupDurationChartComponent(dataReader.runStart, durationsSeries)
			}

			def responseTimeDistributionChartComponent: Component = {
				val (distribution, _) = dataReader.responseTimeDistribution(100, None, Some(group))
				val distributionSeries = new Series[Int, Int]("Group duration", distribution, List(BLUE))

				componentLibrary.getGroupDetailsDurationDistributionChartComponent(distributionSeries)
			}

			def statisticsComponent: Component = new StatisticsTextComponent

			def indicatorChartComponent: Component = componentLibrary.getRequestDetailsIndicatorChartComponent


			// Create template
			val template =
				new GroupDetailsPageTemplate(group,
					responseTimeChartComponent,
					responseTimeDistributionChartComponent,
					statisticsComponent,
					indicatorChartComponent)

			// Write template result to file
			new TemplateWriter(requestFile(runOn, group.path)).writeToFile(template.getOutput)
		}

		dataReader.groupsAndRequests.foreach {
			case (Some(group), None) => generateDetailPage(group)
			case _ => {}
		}
	}
}

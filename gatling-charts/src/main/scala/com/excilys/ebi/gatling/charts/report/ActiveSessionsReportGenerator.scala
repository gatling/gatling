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

import com.excilys.ebi.gatling.charts.component.ComponentLibrary
import com.excilys.ebi.gatling.charts.config.ChartsFiles.activeSessionsFile
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.ActiveSessionsPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ toString, YELLOW, RED, PURPLE, PINK, LIME, LIGHT_RED, LIGHT_PURPLE, LIGHT_PINK, LIGHT_ORANGE, LIGHT_LIME, LIGHT_BLUE, GREEN, CYAN, BLUE }
import com.excilys.ebi.gatling.charts.util.StatisticsHelper.numberOfActiveSessionsPerSecond
import com.excilys.ebi.gatling.core.result.reader.DataReader

class ActiveSessionsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {

		def generatePage(series: Seq[Series[Long, Int]]) {

			val template = new ActiveSessionsPageTemplate(componentLibrary.getActiveSessionsChartComponent(series))

			new TemplateWriter(activeSessionsFile(runOn)).writeToFile(template.getOutput)
		}

		val series = {
			val activeSessionsData = dataReader
				.scenarioNames
				.map { scenarioName => (scenarioName, dataReader.scenarioRequestRecordsGroupByExecutionStartDateInSeconds(scenarioName)) }
				.map { case (scenarioName, scenarioData) => scenarioName -> numberOfActiveSessionsPerSecond(scenarioData) }
				.reverse

			activeSessionsData
				.zip(List(BLUE, GREEN, RED, YELLOW, CYAN, LIME, PURPLE, PINK, LIGHT_BLUE, LIGHT_ORANGE, LIGHT_RED, LIGHT_LIME, LIGHT_PURPLE, LIGHT_PINK))
				.map {
					case ((scenarioName, data), color) => new Series[Long, Int](scenarioName, data, List(color))
				}
		}

		generatePage(series)
	}
}
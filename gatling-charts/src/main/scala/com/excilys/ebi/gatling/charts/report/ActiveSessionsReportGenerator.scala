/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.charts.report
import org.joda.time.DateTime

import com.excilys.ebi.gatling.charts.component.ComponentLibrary
import com.excilys.ebi.gatling.charts.computer.Computer.numberOfActiveSessionsPerSecondByScenario
import com.excilys.ebi.gatling.charts.config.ChartsFiles.activeSessionsFile
import com.excilys.ebi.gatling.charts.loader.DataLoader
import com.excilys.ebi.gatling.charts.series.{ SharedSeries, Series }
import com.excilys.ebi.gatling.charts.template.ActiveSessionsPageTemplate
import com.excilys.ebi.gatling.charts.writer.TemplateWriter
import com.excilys.ebi.gatling.charts.util.Colors._

object ActiveSessionsReportGenerator {
	val ALL_SESSIONS = "All Sessions"
}
class ActiveSessionsReportGenerator(runOn: String, dataLoader: DataLoader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataLoader, componentLibrary) {
	def generate = {
		// Get Data
		val scenariosData = dataLoader.scenarioNames.map { scenarioName =>
			(scenarioName, dataLoader.dataIndexedByScenarioNameAndDateInSeconds(scenarioName))
		} ++ Seq((ActiveSessionsReportGenerator.ALL_SESSIONS, dataLoader.dataIndexedByDateInSeconds))

		val activeSessionsData = numberOfActiveSessionsPerSecondByScenario(scenariosData)

		val colors = List(ORANGE, BLUE, GREEN, RED, YELLOW, CYAN, LIME, PURPLE, PINK, LIGHT_BLUE, LIGHT_ORANGE, LIGHT_RED, LIGHT_LIME, LIGHT_PURPLE, LIGHT_PINK)

		// Create series
		val series = (activeSessionsData.reverse zip colors).map { tuple =>
			val s = new Series[DateTime, Int](tuple._1._1, tuple._1._2, List(tuple._2))
			if (s.name == ActiveSessionsReportGenerator.ALL_SESSIONS)
				SharedSeries.setAllActiveSessionsSeries(s)
			s
		}.toSeq

		// Create template
		val template = new ActiveSessionsPageTemplate(componentLibrary.getActiveSessionsChartComponent(series: _*))

		// Write template result to file
		new TemplateWriter(activeSessionsFile(runOn)).writeToFile(template.getOutput)
	}
}
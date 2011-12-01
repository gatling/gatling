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

object ActiveSessionsReportGenerator {
	val ALL_SESSIONS = "All Sessions"
}
class ActiveSessionsReportGenerator(runOn: String, dataLoader: DataLoader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataLoader, componentLibrary) {
	def generate = {
		// Get Data
		var scenariosData = dataLoader.scenarioNames.map { scenarioName =>
			(scenarioName, dataLoader.dataIndexedByScenarioNameAndDateInSeconds(scenarioName))
		}

		// FIXME!!
		//		scenariosData += (ActiveSessionsReportGenerator.ALL_SESSIONS, dataLoader.dataIndexedByDateInSeconds)

		val activeSessionsData = numberOfActiveSessionsPerSecondByScenario(scenariosData)

		// Create series
		val activeSessionsSeries = activeSessionsData.map { entry =>
			val (seriesName, data) = entry
			val series = new Series[DateTime, Int](seriesName, data)
			if (seriesName == ActiveSessionsReportGenerator.ALL_SESSIONS)
				SharedSeries.setAllActiveSessionsSeries(series)
			series
		}.toSeq.reverse

		// Create template
		val template = new ActiveSessionsPageTemplate(componentLibrary.getActiveSessionsChartComponent(activeSessionsSeries: _*))

		// Write template result to file
		new TemplateWriter(activeSessionsFile(runOn)).writeToFile(template.getOutput)
	}
}
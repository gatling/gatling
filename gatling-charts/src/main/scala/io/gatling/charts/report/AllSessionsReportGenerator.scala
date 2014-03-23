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

import io.gatling.charts.component.ComponentLibrary
import io.gatling.charts.config.ChartsFiles.allSessionsFile
import io.gatling.charts.util.Colors.ORANGE
import io.gatling.core.result.{ IntVsTimePlot, Series }
import io.gatling.core.result.reader.DataReader

class AllSessionsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate(): Unit = {
		val series = new Series[IntVsTimePlot]("All Sessions", dataReader.numberOfActiveSessionsPerSecond(), List(ORANGE))

		val javascript = componentLibrary.getAllSessionsJs(dataReader.runStart, series)

		new TemplateWriter(allSessionsFile(runOn)).writeToFile(javascript)
	}
}

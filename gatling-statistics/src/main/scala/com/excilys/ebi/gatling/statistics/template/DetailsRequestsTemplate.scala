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
package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

import com.excilys.ebi.gatling.core.util.PathHelper._

import com.excilys.ebi.gatling.statistics.result.DetailsRequestsDataResult

class DetailsRequestsTemplate(val runOn: String, val series: List[TimeSeries], val columnData: ColumnSeries, val requestName: String, val result: DetailsRequestsDataResult) {

	val bodyEngine = new TemplateEngine
	bodyEngine.escapeMarkup = false

	def getOutput: String = {
		val plotBand = new PlotBand(0, 0)
		val highstocks = new HighstocksTimeTemplate(series, "Response Time", "Response Time in ms", "Response Time of {}ms", plotBand).getOutput +
			new HighstocksColumnTemplate(columnData, "Dispersion", "Number of Requests", "{} Requests").getOutput

		val body = bodyEngine.layout(GATLING_TEMPLATE_REQUEST_DETAILS_BODY_FILE,
			Map("result" -> result))

		new LayoutTemplate("Details for: " + requestName, runOn, body, highstocks).getOutput
	}
}
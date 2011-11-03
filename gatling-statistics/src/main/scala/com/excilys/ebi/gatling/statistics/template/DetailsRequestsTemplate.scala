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
import com.excilys.ebi.gatling.statistics.series.TimeSeries
import com.excilys.ebi.gatling.statistics.series.ColumnSeries
import com.excilys.ebi.gatling.statistics.series.PlotBand
import com.excilys.ebi.gatling.statistics.series.YAxis
import com.excilys.ebi.gatling.statistics.template.ChartContainer._
import com.excilys.ebi.gatling.statistics.series.PieSeries

class DetailsRequestsTemplate(val runOn: String, val series: List[TimeSeries], val dispersionData: ColumnSeries, val indicatorsData: ColumnSeries, val pieData: PieSeries,
		val requestName: String, val result: DetailsRequestsDataResult) {

	val bodyEngine = new TemplateEngine
	bodyEngine.escapeMarkup = false

	def getOutput: String = {
		val plotBand = new PlotBand(0, 0)
		val highstocks = new HighstocksTimeTemplate(series, "Response Time", List(new YAxis("Active Sessions", " users", true, "#4572A7"), new YAxis("Response Time", "ms", false, "#89A54E")), "Response Time of {}ms", plotBand).getOutput +
			//new HighstocksColumnTemplate(dispersionData, DISPERSION_CHART, "Dispersion", "Number of Requests", "{} Requests").getOutput +
			new HighstocksColumnTemplate(indicatorsData, INDICATORS_CHART, "Number of Requests", "Number of Requests", "{} Requests").getOutput

		val body = bodyEngine.layout(GATLING_TEMPLATE_REQUEST_DETAILS_BODY_FILE,
			Map("result" -> result))

		new LayoutTemplate("Details for: " + requestName, runOn, body, highstocks).getOutput
	}
}
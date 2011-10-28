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

import com.excilys.ebi.gatling.core.util.PathHelper._
import org.fusesource.scalate.TemplateEngine

private[template] class HighstocksTimeTemplate(val series: List[TimeSeries], val chartTitle: String, val yAxisTitle: String, val toolTip: String, val plotBand: PlotBand) {
	def this(series: List[TimeSeries], chartTitle: String, yAxisTitle: String, toolTip: String) = this(series, chartTitle, yAxisTitle, toolTip, new PlotBand(0, 0))

	val highstocksEngine = new TemplateEngine
	highstocksEngine.escapeMarkup = false

	def getOutput: String = {
		highstocksEngine.layout(GATLING_TEMPLATE_HIGHSTOCKS_TIME_FILE,
			Map("series" -> series,
				"chartTitle" -> chartTitle,
				"yAxisTitle" -> yAxisTitle,
				"toolTip" -> toolTip,
				"hasPlotBand" -> (plotBand.maxValue != plotBand.minValue),
				"plotBand" -> plotBand))
	}
}
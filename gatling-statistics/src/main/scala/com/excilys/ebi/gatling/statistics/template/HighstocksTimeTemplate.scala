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
import com.excilys.ebi.gatling.statistics.series.PlotBand
import com.excilys.ebi.gatling.statistics.series.Series
import com.excilys.ebi.gatling.statistics.series.YAxis

private[template] class HighstocksTimeTemplate(val series: List[Series], val chartTitle: String, val yAxis: List[YAxis], val toolTip: String, val plotBand: PlotBand) {
	def this(series: List[Series], chartTitle: String, yAxis: List[YAxis], toolTip: String) = this(series, chartTitle, yAxis, toolTip, new PlotBand(0, 0))

	val highstocksEngine = new TemplateEngine
	highstocksEngine.escapeMarkup = false

	def getOutput: String = {
		highstocksEngine.layout(GATLING_TEMPLATE_HIGHSTOCKS_TIME_FILE,
			Map("series" -> series,
				"chartTitle" -> chartTitle,
				"yAxis" -> yAxis,
				"toolTip" -> toolTip,
				"hasPlotBand" -> (plotBand.maxValue != plotBand.minValue),
				"plotBand" -> plotBand))
	}
}
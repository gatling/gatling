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
import com.excilys.ebi.gatling.statistics.series.ColumnSeries

private[template] class HighchartsColumnTemplate(val columnData: ColumnSeries, val container: String, val chartTitle: String, val yAxisTitle: String, val toolTip: String) {

	val highstocksEngine = new TemplateEngine
	highstocksEngine.escapeMarkup = false

	def getOutput: String = {
		highstocksEngine.layout(GATLING_TEMPLATE_HIGHCHARTS_COLUMN_FILE,
			Map("columnData" -> columnData,
				"container" -> container,
				"chartTitle" -> chartTitle,
				"yAxisTitle" -> yAxisTitle,
				"toolTip" -> toolTip,
				"xCategories" -> columnData.categories))
	}

}
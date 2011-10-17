package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

import com.excilys.ebi.gatling.core.util.PathHelper._

private[template] class HighchartsColumnTemplate(val columnData: ColumnSeries, val graphTitle: String, val yAxisTitle: String, val toolTip: String) {

	val highchartsEngine = new TemplateEngine
	highchartsEngine.escapeMarkup = false

	def getOutput: String = {
		highchartsEngine.layout(GATLING_TEMPLATE_HIGHCHARTS_COLUMN_FILE,
			Map("columnData" -> columnData,
				"graphTitle" -> graphTitle,
				"yAxisTitle" -> yAxisTitle,
				"toolTip" -> toolTip,
				"xCategories" -> columnData.categories))
	}

}
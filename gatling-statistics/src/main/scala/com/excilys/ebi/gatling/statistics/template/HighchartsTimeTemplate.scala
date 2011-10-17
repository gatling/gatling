package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

import com.excilys.ebi.gatling.core.util.PathHelper._

private[template] class HighchartsTimeTemplate(val series: List[TimeSeries], val graphTitle: String, val yAxisTitle: String, val toolTip: String, val plotBand: PlotBand) {

	def this(series: List[TimeSeries], graphTitle: String, yAxisTitle: String, toolTip: String) = this(series, graphTitle, yAxisTitle, toolTip, new PlotBand(0, 0))

	val highchartsEngine = new TemplateEngine
	highchartsEngine.escapeMarkup = false

	def getOutput: String = {
		highchartsEngine.layout(GATLING_TEMPLATE_HIGHCHARTS_TIME_FILE,
			Map("series" -> series,
				"graphTitle" -> graphTitle,
				"yAxisTitle" -> yAxisTitle,
				"toolTip" -> toolTip,
				"hasPlotBand" -> (plotBand.maxValue != plotBand.minValue),
				"plotBand" -> plotBand))
	}
}
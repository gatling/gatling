package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

private[template] class HighchartsTemplate(val series: List[Series], val graphTitle: String, val yAxisTitle: String, val toolTip: String, val plotBand: PlotBand) {

  def this(series: List[Series], graphTitle: String, yAxisTitle: String, toolTip: String) = this(series, graphTitle, yAxisTitle, toolTip, new PlotBand(0, 0))

  val highchartsEngine = new TemplateEngine
  highchartsEngine.escapeMarkup = false

  def getOutput: String = {
    highchartsEngine.layout("templates/highcharts.ssp",
      Map("series" -> series,
        "graphTitle" -> graphTitle,
        "yAxisTitle" -> yAxisTitle,
        "toolTip" -> toolTip,
        "hasPlotBand" -> (plotBand.maxValue != plotBand.minValue),
        "plotBand" -> plotBand))
  }
}
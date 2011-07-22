package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

class HighchartsTemplate(val series: List[Series], val graphTitle: String, val yAxisTitle: String, val toolTip: String) {

  val highchartsEngine = new TemplateEngine

  highchartsEngine.bindings = List(
    Binding("series", "List[com.excilys.ebi.gatling.statistics.template.Series]"),
    Binding("graphTitle", "String"),
    Binding("yAxisTitle", "String"),
    Binding("toolTip", "String"))
  highchartsEngine.escapeMarkup = false

  def getOutput: String = {
    highchartsEngine.layout("templates/highcharts.ssp",
      Map("series" -> series,
        "graphTitle" -> graphTitle,
        "yAxisTitle" -> yAxisTitle,
        "toolTip" -> toolTip))
  }
}
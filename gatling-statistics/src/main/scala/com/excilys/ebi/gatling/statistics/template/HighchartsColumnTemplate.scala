package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

private[template] class HighchartsColumnTemplate(val columnData: ColumnSeries, val graphTitle: String, val yAxisTitle: String, val toolTip: String) {

  val highchartsEngine = new TemplateEngine
  highchartsEngine.escapeMarkup = false

  def getOutput: String = {
    highchartsEngine.layout("templates/highcharts_column.ssp",
      Map("columnData" -> columnData,
        "graphTitle" -> graphTitle,
        "yAxisTitle" -> yAxisTitle,
        "toolTip" -> toolTip,
        "xCategories" -> columnData.categories))
  }

}
package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

import com.excilys.ebi.gatling.core.util.PathHelper._

import com.excilys.ebi.gatling.statistics.result.DetailsRequestsDataResult

class DetailsRequestsTemplate(val runOn: String, val series: List[TimeSeries], val columnData: ColumnSeries, val requestName: String, val result: DetailsRequestsDataResult) {

  val bodyEngine = new TemplateEngine
  bodyEngine.escapeMarkup = false

  def getOutput: String = {
    val plotBand = new PlotBand(result.medium - result.standardDeviation, result.medium + result.standardDeviation)
    val highcharts =
      new HighchartsTimeTemplate(series, "Response Time", "Response Time in ms", "Response Time of {}ms", plotBand).getOutput +
        new HighchartsColumnTemplate(columnData, "Dispersion", "Number of Requests", "{} Requests").getOutput

    val body = bodyEngine.layout(GATLING_TEMPLATE_REQUEST_DETAILS_BODY_FILE,
      Map("requestName" -> requestName, "result" -> result))

    new LayoutTemplate("Details of '" + requestName + "'", runOn, body, highcharts).getOutput
  }
}
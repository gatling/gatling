package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

import com.excilys.ebi.gatling.statistics.result.DetailsRequestsDataResult

class DetailsRequestsTemplate(val runOn: String, val menuItems: Map[String, String], val series: List[Series], val requestName: String, val result: DetailsRequestsDataResult) {

  val bodyEngine = new TemplateEngine

  bodyEngine.bindings = List(
    Binding("requestName", "String"),
    Binding("result", "com.excilys.ebi.gatling.statistics.result.DetailsRequestsDataResult"))
  bodyEngine.escapeMarkup = false

  def getOutput: String = {
    val highcharts = new HighchartsTemplate(series, "Response Time", "Response Time in ms", "Response Time of {}ms").getOutput

    val body = bodyEngine.layout("templates/details_requests_body.ssp",
      Map("requestName" -> requestName, "result" -> result))

    new LayoutTemplate("Details of '" + requestName + "'", runOn, body, highcharts, menuItems).getOutput
  }
}
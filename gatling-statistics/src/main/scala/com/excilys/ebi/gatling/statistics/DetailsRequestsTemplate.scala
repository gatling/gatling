package com.excilys.ebi.gatling.statistics

import org.fusesource.scalate._

class DetailsRequestsTemplate(val runOn: String, val menuItems: Map[String, String], val dates: List[String], val values: List[Int], val requestName: String, val result: DetailsRequestsDataResult) {

  val highchartsEngine = new TemplateEngine
  val bodyEngine = new TemplateEngine

  highchartsEngine.bindings = List(
    Binding("dates", "List[String]"),
    Binding("values", "List[Int]"))
  highchartsEngine.escapeMarkup = false

  bodyEngine.bindings = List(
    Binding("requestName", "String"),
    Binding("result", "com.excilys.ebi.gatling.statistics.DetailsRequestsDataResult"))
  bodyEngine.escapeMarkup = false

  def getOutput: String = {
    val highcharts = highchartsEngine.layout("templates/details_requests_highcharts.ssp",
      Map("dates" -> dates, "values" -> values))

    val body = bodyEngine.layout("templates/details_requests_body.ssp",
      Map("requestName" -> requestName, "result" -> result))

    new LayoutTemplate("Details of '" + requestName + "'", runOn, body, highcharts, menuItems).getOutput
  }
}
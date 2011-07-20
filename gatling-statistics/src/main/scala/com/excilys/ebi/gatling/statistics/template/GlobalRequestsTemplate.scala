package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

class GlobalRequestsTemplate(val runOn: String, val menuItems: Map[String, String], val dates: List[String], val globalValues: List[Int], val okValues: List[Int], val koValues: List[Int]) {

  val highchartsEngine = new TemplateEngine

  highchartsEngine.bindings = List(
    Binding("dates", "List[String]"),
    Binding("globalValues", "List[Int]"),
    Binding("okValues", "List[Int]"),
    Binding("koValues", "List[Int]"))
  highchartsEngine.escapeMarkup = false

  def getOutput: String = {
    val highcharts = highchartsEngine.layout("templates/global_requests_highcharts.ssp",
      Map("dates" -> dates,
        "globalValues" -> globalValues,
        "okValues" -> okValues,
        "koValues" -> koValues))

    new LayoutTemplate("Requests", runOn, "", highcharts, menuItems).getOutput
  }

}
package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

class ActiveSessionsTemplate(val runOn: String, val menuItems: Map[String, String], val dates: List[String], val values: List[Int]) {

  val highchartsEngine = new TemplateEngine

  highchartsEngine.bindings = List(
    Binding("dates", "List[String]"),
    Binding("values", "List[Int]"))
  highchartsEngine.escapeMarkup = false

  def getOutput: String = {
    val highcharts = highchartsEngine.layout("templates/active_sessions_highcharts.ssp",
      Map("dates" -> dates, "values" -> values))

    new LayoutTemplate("Active Sessions", runOn, "", highcharts, menuItems).getOutput
  }

}
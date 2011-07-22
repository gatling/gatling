package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

class ActiveSessionsTemplate(val runOn: String, val menuItems: Map[String, String], val data: List[(String, Int)]) {

  val highchartsEngine = new TemplateEngine

  highchartsEngine.bindings = List(
    Binding("data", "List[(String,Int)]"))
  highchartsEngine.escapeMarkup = false

  def getOutput: String = {
    val highcharts = highchartsEngine.layout("templates/active_sessions_highcharts.ssp",
      Map("data" -> data))

    new LayoutTemplate("Active Sessions", runOn, "", highcharts, menuItems).getOutput
  }

}
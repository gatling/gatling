package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

class GlobalRequestsTemplate(val runOn: String, val menuItems: Map[String, String], val globalData: List[(String, Int)], val successData: List[(String, Int)], val failureData: List[(String, Int)]) {

  val highchartsEngine = new TemplateEngine

  highchartsEngine.bindings = List(
    Binding("globalData", "List[(String,Int)]"),
    Binding("successData", "List[(String,Int)]"),
    Binding("failureData", "List[(String,Int)]"))
  highchartsEngine.escapeMarkup = false

  def getOutput: String = {
    val highcharts = highchartsEngine.layout("templates/global_requests_highcharts.ssp",
      Map("globalData" -> globalData,
        "successData" -> successData,
        "failureData" -> failureData))

    new LayoutTemplate("Requests", runOn, "", highcharts, menuItems).getOutput
  }

}
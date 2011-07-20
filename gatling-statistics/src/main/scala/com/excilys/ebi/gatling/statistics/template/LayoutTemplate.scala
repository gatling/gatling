package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

class LayoutTemplate(val title: String, val runOn: String, val body: String, val highcharts: String, val menuItems: Map[String, String]) {

  val engine = new TemplateEngine

  engine.bindings = List(
    Binding("title", "String"),
    Binding("runOn", "String"),
    Binding("body", "String"),
    Binding("highcharts", "String"),
    Binding("menuItems", "Map[String, String]"))
  engine.escapeMarkup = false

  def getOutput: String = {
    engine.layout("templates/layout.ssp",
      Map("title" -> title,
        "runOn" -> runOn,
        "body" -> body,
        "highcharts" -> highcharts,
        "menuItems" -> menuItems))
  }
}
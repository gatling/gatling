package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

import com.excilys.ebi.gatling.core.util.PathHelper._

private[template] class LayoutTemplate(val title: String, val runOn: String, val body: String, val highcharts: String) {

  val engine = new TemplateEngine
  engine.escapeMarkup = false

  def getOutput: String = {
    engine.layout(GATLING_TEMPLATE_LAYOUT_FILE,
      Map("title" -> title,
        "runOn" -> runOn,
        "body" -> body,
        "highcharts" -> highcharts))
  }
}
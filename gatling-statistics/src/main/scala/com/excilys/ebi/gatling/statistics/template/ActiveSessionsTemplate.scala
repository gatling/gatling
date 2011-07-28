package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

class ActiveSessionsTemplate(val runOn: String, val menuItems: Map[String, String], val series: List[Series]) {

  def getOutput: String = {
    val highcharts = new HighchartsTemplate(series, "Active Sessions", "Active sessions", "{} users").getOutput

    new LayoutTemplate("Active Sessions", runOn, "", highcharts, menuItems).getOutput
  }

}
package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

class ActiveSessionsTemplate(val runOn: String, val menuItems: Map[String, String], val series: List[TimeSeries]) {

  def getOutput: String = {
    val highcharts = new HighchartsTimeTemplate(series, "Active Sessions", "Active sessions", "{} users").getOutput

    new LayoutTemplate("Active Sessions", runOn, "", highcharts, menuItems).getOutput
  }

}
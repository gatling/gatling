package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

class GlobalRequestsTemplate(val runOn: String, val menuItems: Map[String, String], val series: List[Series]) {

  def getOutput: String = {
    val highcharts = new HighchartsTemplate(series, "Number of Requests", "Number of requests per second", "{} requests").getOutput
    new LayoutTemplate("Requests", runOn, "", highcharts, menuItems).getOutput
  }

}
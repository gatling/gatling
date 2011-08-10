package com.excilys.ebi.gatling.statistics.template

import org.fusesource.scalate._

class GlobalRequestsTemplate(val runOn: String, val menuItems: Map[String, String], val series: List[TimeSeries]) {

  def getOutput: String = {
    val highcharts = new HighchartsTimeTemplate(series, "Number of Requests", "Number of requests per second", "{} requests").getOutput
    new LayoutTemplate("Requests", runOn, "", highcharts, menuItems).getOutput
  }

}
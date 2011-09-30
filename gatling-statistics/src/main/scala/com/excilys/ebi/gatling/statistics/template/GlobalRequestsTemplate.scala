package com.excilys.ebi.gatling.statistics.template
import org.apache.commons.lang3.StringUtils

class GlobalRequestsTemplate(val runOn: String, val series: List[TimeSeries]) {

  def getOutput: String = {
    val highcharts = new HighchartsTimeTemplate(series, "Number of Requests", "Number of requests per second", "{} requests").getOutput
    new LayoutTemplate("Requests", runOn, StringUtils.EMPTY, highcharts).getOutput
  }

}
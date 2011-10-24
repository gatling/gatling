package com.excilys.ebi.gatling.statistics.template

import com.excilys.ebi.gatling.core.util.StringHelper._

class GlobalRequestsTemplate(val runOn: String, val series: List[TimeSeries]) {

	def getOutput: String = {
		val highcharts = new HighchartsTimeTemplate(series, "Number of Requests", "Number of requests per second", "{} requests").getOutput
		new LayoutTemplate("Requests", runOn, EMPTY, highcharts).getOutput
	}

}
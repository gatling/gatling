package com.excilys.ebi.gatling.statistics.template
import com.excilys.ebi.gatling.core.util.StringHelper._

class ActiveSessionsTemplate(val runOn: String, val series: List[TimeSeries]) {

	def getOutput: String = {
		val highcharts = new HighchartsTimeTemplate(series, "Active Sessions", "Active sessions", "{} users").getOutput

		new LayoutTemplate("Active Sessions", runOn, EMPTY, highcharts).getOutput
	}

}
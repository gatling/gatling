package com.excilys.ebi.gatling.statistics.template
import org.apache.commons.lang3.StringUtils

class ActiveSessionsTemplate(val runOn: String, val series: List[TimeSeries]) {

	def getOutput: String = {
		val highcharts = new HighchartsTimeTemplate(series, "Active Sessions", "Active sessions", "{} users").getOutput

		new LayoutTemplate("Active Sessions", runOn, StringUtils.EMPTY, highcharts).getOutput
	}

}
/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.charts.template

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.charts.config.ChartsFiles.GATLING_TEMPLATE_STATS_TSV_FILE_URL
import com.excilys.ebi.gatling.charts.report.GroupContainer

object StatsTsvTemplate {
	val headers = Vector("name", "nbRequest", "nbRequestOK", "nbRequestKO",
		"minResponseTime", "minResponseTimeOK", "minResponseTimeKO",
		"maxResponseTime", "maxResponseTimeOK", "maxResponseTimeKO",
		"meanResponseTime", "meanResponseTimeOK", "meanResponseTimeKO",
		"deviation", "deviationOK", "deviationKO",
		"percentile1", "percentile1OK", "percentile1KO",
		"percentile2", "percentile2OK", "percentile2KO",
		"group1Name", "group1Count", "group1Percentage",
		"group2Name", "group2Count", "group2Percentage",
		"group3Name", "group3Count", "group3Percentage",
		"group4Name", "group4Count", "group4Percentage",
		"meanNumberOfRequestsPerSecond", "meanNumberOfRequestsPerSecondOK", "meanNumberOfRequestsPerSecondKO")
}

class StatsTsvTemplate(stats: GroupContainer) {

	def getOutput: String = {
		val headers = StatsTsvTemplate.headers.mkString(configuration.charting.statsTsvSeparator)
		PageTemplate.TEMPLATE_ENGINE.layout(GATLING_TEMPLATE_STATS_TSV_FILE_URL, Map("headers" -> headers ,"stats" -> stats))
	}
}

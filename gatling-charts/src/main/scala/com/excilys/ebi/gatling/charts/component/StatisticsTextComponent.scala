/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.charts.component

import com.excilys.ebi.gatling.charts.config.ChartsFiles.GATLING_TEMPLATE_STATISTICS_COMPONENT_URL
import com.excilys.ebi.gatling.charts.template.PageTemplate.TEMPLATE_ENGINE
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.charts.computer.Computer.NO_PLOT_MAGIC_VALUE

case class Statistics(val name: String, val total: Long, val success: Long, val failure: Long) {
	val printableTotal: String = if (total != NO_PLOT_MAGIC_VALUE) total.toString else "-"
	val printableSuccess: String = if (success != NO_PLOT_MAGIC_VALUE) success.toString else "-"
	val printableFailure: String = if (failure != NO_PLOT_MAGIC_VALUE) failure.toString else "-"
}

class StatisticsTextComponent(statistics: Statistics*)
		extends Component {

	def getHTMLContent: String = TEMPLATE_ENGINE.layout(GATLING_TEMPLATE_STATISTICS_COMPONENT_URL, statistics.map(stats => (stats.name, stats)).toMap[String, Statistics])

	def getJavascriptContent: String = EMPTY

	def getJavascriptFiles: Seq[String] = Seq.empty
}
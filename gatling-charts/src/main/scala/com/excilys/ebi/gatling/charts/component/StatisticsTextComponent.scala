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
import com.excilys.ebi.gatling.charts.util.StatisticsHelper.NO_PLOT_MAGIC_VALUE
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

case class Statistics(name: String, total: Long, success: Long, failure: Long) {

	private def makePrintable(value: Long) = if (value != NO_PLOT_MAGIC_VALUE) value.toString else "-"

	val printableTotal: String = makePrintable(total)

	val printableSuccess: String = makePrintable(success)

	val printableFailure: String = makePrintable(failure)
}

class StatisticsTextComponent(statistics: Statistics*) extends Component {

	val statisticsIndexedByName = statistics.map(stats => (stats.name, stats)).toMap[String, Statistics]

	def getHTMLContent: String = TEMPLATE_ENGINE.layout(GATLING_TEMPLATE_STATISTICS_COMPONENT_URL, statisticsIndexedByName)

	def getJavascriptContent: String = EMPTY

	def getJavascriptFiles: Seq[String] = Seq.empty
}
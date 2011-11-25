/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.charts.component

import com.excilys.ebi.gatling.charts.template.PageTemplate
import com.excilys.ebi.gatling.charts.util.PathHelper.GATLING_CHARTS_STATISTICS_TEMPLATE
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

class StatisticsTextComponent(numberOfRequest: Int, minResponseTime: Int, maxResponseTime: Int, averageResponseTime: Double, responseTimeStandardDeviation: Double)
		extends Component {

	def getHTMLContent: String = {
		PageTemplate.TEMPLATE_ENGINE.layout(GATLING_CHARTS_STATISTICS_TEMPLATE,
			Map("numberOfRequests" -> numberOfRequest,
				"min" -> minResponseTime,
				"max" -> maxResponseTime,
				"average" -> averageResponseTime,
				"stdDeviation" -> responseTimeStandardDeviation))
	}

	def getJavascriptContent: String = EMPTY

	def getJavascriptFiles: Seq[String] = Seq.empty
}
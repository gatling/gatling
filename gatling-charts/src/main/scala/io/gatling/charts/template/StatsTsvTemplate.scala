/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.charts.template

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.charts.report.{ GroupContainer, RequestContainer }

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

		def renderGroup(group: GroupContainer): Fastring = fast"""${group.stats.mkString}
${
			(group.contents.values.map {
				_ match {
					case subGroup: GroupContainer => renderGroup(subGroup)
					case request: RequestContainer => request.stats.mkString
				}
			}).mkFastring("\n")
		}"""

		val headers = StatsTsvTemplate.headers.mkString(configuration.charting.statsTsvSeparator)

		fast"""$headers
${renderGroup(stats)}
""".toString

	}
}

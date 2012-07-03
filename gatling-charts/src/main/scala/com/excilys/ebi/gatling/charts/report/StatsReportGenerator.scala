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
package com.excilys.ebi.gatling.charts.report

import com.excilys.ebi.gatling.charts.component.{ StatisticsTextComponent, Statistics, RequestStatistics, ComponentLibrary }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.reader.{ DataReader, ChartRequestRecord }
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ OK, KO }
import com.excilys.ebi.gatling.charts.config.ChartsFiles.{ jsStatsFile, tsvStatsFile, GLOBAL_PAGE_NAME }
import com.excilys.ebi.gatling.charts.template.StatsJsTemplate
import com.excilys.ebi.gatling.charts.template.StatsTsvTemplate

class StatsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) {

	def generate: Map[String, RequestStatistics] = {
		val criteria: List[(String, Option[String])] = (GLOBAL_PAGE_NAME, None) :: dataReader.requestNames.map(name => (name, Some(name))).toList

		val percent1 = configuration.chartingIndicatorsPercentile1 / 100.0
		val percent2 = configuration.chartingIndicatorsPercentile2 / 100.0

		val stats = criteria.map {
			case (name, requestName) =>

				val totalCount = dataReader.countRequests(None, requestName)
				val okCount = dataReader.countRequests(Some(OK), requestName)
				val koCount = totalCount - okCount

				val globalMinResponseTime = dataReader.minResponseTime(None, requestName)
				val globalMaxResponseTime = dataReader.maxResponseTime(None, requestName)
				val okMinResponseTime = dataReader.minResponseTime(Some(OK), requestName)
				val okMaxResponseTime = dataReader.maxResponseTime(Some(OK), requestName)
				val koMinResponseTime = dataReader.minResponseTime(Some(KO), requestName)
				val koMaxResponseTime = dataReader.maxResponseTime(Some(KO), requestName)

				val globalMeanResponseTime = dataReader.meanResponseTime(None, requestName)
				val okMeanResponseTime = dataReader.meanResponseTime(Some(OK), requestName)
				val koMeanResponseTime = dataReader.meanResponseTime(Some(KO), requestName)

				val globalStandardDeviation = dataReader.responseTimeStandardDeviation(None, requestName)
				val okStandardDeviation = dataReader.responseTimeStandardDeviation(Some(OK), requestName)
				val koStandardDeviation = dataReader.responseTimeStandardDeviation(Some(KO), requestName)

				val (globalPercentile1, globalPercentile2) = dataReader.percentiles(percent1, percent2)
				val (successPercentile1, successPercentile2) = dataReader.percentiles(percent1, percent2, Some(OK))
				val (failedPercentile1, failedPercentile2) = dataReader.percentiles(percent1, percent2, Some(KO))

				val numberOfRequestsStatistics = Statistics("numberOfRequests", totalCount, okCount, koCount)
				val minResponseTimeStatistics = Statistics("min", globalMinResponseTime, okMinResponseTime, koMinResponseTime)
				val maxResponseTimeStatistics = Statistics("max", globalMaxResponseTime, okMaxResponseTime, koMaxResponseTime)
				val meanStatistics = Statistics("mean", globalMeanResponseTime, okMeanResponseTime, koMeanResponseTime)
				val stdDeviationStatistics = Statistics("stdDeviation", globalStandardDeviation, okStandardDeviation, koStandardDeviation)
				val percentiles1 = Statistics("percentiles1", globalPercentile1, successPercentile1, failedPercentile1)
				val percentiles2 = Statistics("percentiles2", globalPercentile2, successPercentile2, failedPercentile2)

				(name -> RequestStatistics(name, numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, meanStatistics, stdDeviationStatistics, percentiles1, percentiles2))
		}.toMap

		new TemplateWriter(jsStatsFile(runOn)).writeToFile(new StatsJsTemplate(stats).getOutput)
		new TemplateWriter(tsvStatsFile(runOn)).writeToFile(new StatsTsvTemplate(stats).getOutput)

		stats
	}
}
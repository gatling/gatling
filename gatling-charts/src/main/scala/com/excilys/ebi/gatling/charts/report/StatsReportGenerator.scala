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

		val percent1 = configuration.charting.indicators.percentile1 / 100.0
		val percent2 = configuration.charting.indicators.percentile2 / 100.0

		val stats = criteria.map {
			case (name, requestName) =>

				val totalCount = dataReader.countRequests(None, requestName)
				val okCount = dataReader.countRequests(Some(OK), requestName)
				val koCount = totalCount - okCount

				val globalMinResponseTime = dataReader.minResponseTime(None, requestName)
				val okMinResponseTime = dataReader.minResponseTime(Some(OK), requestName)
				val koMinResponseTime = dataReader.minResponseTime(Some(KO), requestName)

				val globalMaxResponseTime = dataReader.maxResponseTime(None, requestName)
				val okMaxResponseTime = dataReader.maxResponseTime(Some(OK), requestName)
				val koMaxResponseTime = dataReader.maxResponseTime(Some(KO), requestName)

				val globalMeanResponseTime = dataReader.meanResponseTime(None, requestName)
				val okMeanResponseTime = dataReader.meanResponseTime(Some(OK), requestName)
				val koMeanResponseTime = dataReader.meanResponseTime(Some(KO), requestName)

				val globalStandardDeviation = dataReader.responseTimeStandardDeviation(None, requestName)
				val okStandardDeviation = dataReader.responseTimeStandardDeviation(Some(OK), requestName)
				val koStandardDeviation = dataReader.responseTimeStandardDeviation(Some(KO), requestName)

				val (globalPercentile1, globalPercentile2) = dataReader.percentiles(percent1, percent2, None, requestName)
				val (successPercentile1, successPercentile2) = dataReader.percentiles(percent1, percent2, Some(OK), requestName)
				val (failedPercentile1, failedPercentile2) = dataReader.percentiles(percent1, percent2, Some(KO), requestName)

				val globalMeanNumberOfRequestsPerSecond = dataReader.meanNumberOfRequestsPerSecond(None, requestName)
				val okMeanNumberOfRequestsPerSecond = dataReader.meanNumberOfRequestsPerSecond(Some(OK), requestName)
				val koMeanNumberOfRequestsPerSecond = dataReader.meanNumberOfRequestsPerSecond(Some(KO), requestName)

				val numberOfRequestsStatistics = Statistics("numberOfRequests", totalCount, okCount, koCount)
				val minResponseTimeStatistics = Statistics("minResponseTime", globalMinResponseTime, okMinResponseTime, koMinResponseTime)
				val maxResponseTimeStatistics = Statistics("maxResponseTime", globalMaxResponseTime, okMaxResponseTime, koMaxResponseTime)
				val meanResponseTimeStatistics = Statistics("meanResponseTime", globalMeanResponseTime, okMeanResponseTime, koMeanResponseTime)
				val stdDeviationStatistics = Statistics("stdDeviation", globalStandardDeviation, okStandardDeviation, koStandardDeviation)
				val percentiles1 = Statistics("percentiles1", globalPercentile1, successPercentile1, failedPercentile1)
				val percentiles2 = Statistics("percentiles2", globalPercentile2, successPercentile2, failedPercentile2)
				val meanNumberOfRequestsPerSecondStatistics = Statistics("meanNumberOfRequestsPerSecond", globalMeanNumberOfRequestsPerSecond, okMeanNumberOfRequestsPerSecond, koMeanNumberOfRequestsPerSecond)

				val groupedCounts = dataReader
					.numberOfRequestInResponseTimeRange(configuration.charting.indicators.lowerBound, configuration.charting.indicators.higherBound, requestName)
					.map { case (name, count) => (name, count, count * 100 / totalCount) }

				(name -> RequestStatistics(name, numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, meanResponseTimeStatistics, stdDeviationStatistics, percentiles1, percentiles2, groupedCounts, meanNumberOfRequestsPerSecondStatistics))
		}.toMap

		new TemplateWriter(jsStatsFile(runOn)).writeToFile(new StatsJsTemplate(stats).getOutput)
		new TemplateWriter(tsvStatsFile(runOn)).writeToFile(new StatsTsvTemplate(stats).getOutput)

		stats
	}
}
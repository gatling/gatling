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

import com.excilys.ebi.gatling.charts.component.{ StatisticsTextComponent, Statistics, ComponentLibrary }
import com.excilys.ebi.gatling.charts.config.ChartsFiles.globalFile
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.GlobalPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ toString, YELLOW, RED, PURPLE, PINK, ORANGE, LIME, LIGHT_RED, LIGHT_PURPLE, LIGHT_PINK, LIGHT_ORANGE, LIGHT_LIME, LIGHT_BLUE, GREEN, CYAN, BLUE }
import com.excilys.ebi.gatling.charts.util.StatisticsHelper.count
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ OK, KO }
import com.excilys.ebi.gatling.core.result.reader.{ DataReader, ChartRequestRecord }
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

class GlobalReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		val totalCount = dataReader.countRequests()
		val okCount = dataReader.countRequests(Some(OK))
		val koCount = totalCount - okCount
		val globalMinResponseTime = dataReader.minResponseTime()
		val globalMaxResponseTime = dataReader.maxResponseTime()
		val okMinResponseTime = dataReader.minResponseTime(Some(OK))
		val okMaxResponseTime = dataReader.maxResponseTime(Some(OK))
		val koMinResponseTime = dataReader.minResponseTime(Some(KO))
		val koMaxResponseTime = dataReader.maxResponseTime(Some(KO))

		def activeSessionsChartComponent = {
			val activeSessionsSeries = dataReader
				.scenarioNames
				.map { scenarioName => scenarioName -> dataReader.numberOfActiveSessionsPerSecond(Some(scenarioName)) }
				.reverse
				.zip(List(BLUE, GREEN, RED, YELLOW, CYAN, LIME, PURPLE, PINK, LIGHT_BLUE, LIGHT_ORANGE, LIGHT_RED, LIGHT_LIME, LIGHT_PURPLE, LIGHT_PINK))
				.map { case ((scenarioName, data), color) => new Series[Long, Int](scenarioName, data, List(color)) }

			componentLibrary.getActiveSessionsChartComponent(activeSessionsSeries)
		}

		def requestsChartComponent = {
			val all = dataReader.numberOfEventsPerSecond((record: ChartRequestRecord) => record.executionStartDateNoMillis).toSeq.sortBy(_._1)
			val oks = dataReader.numberOfEventsPerSecond((record: ChartRequestRecord) => record.executionStartDateNoMillis, Some(OK)).toSeq.sortBy(_._1)
			val kos = dataReader.numberOfEventsPerSecond((record: ChartRequestRecord) => record.executionStartDateNoMillis, Some(KO)).toSeq.sortBy(_._1)

			val allSeries = new Series[Long, Int]("All requests", all, List(BLUE))
			val kosSeries = new Series[Long, Int]("Failed requests", kos, List(RED))
			val oksSeries = new Series[Long, Int]("Succeeded requests", oks, List(GREEN))
			val pieRequestsSeries = new Series[String, Int]("Distribution", ("Success", count(oks)) :: ("Failures", count(kos)) :: Nil, List(GREEN, RED))

			componentLibrary.getRequestsChartComponent(allSeries, kosSeries, oksSeries, pieRequestsSeries)
		}

		def transactionsChartComponent = {
			val all = dataReader.numberOfEventsPerSecond((record: ChartRequestRecord) => record.executionEndDateNoMillis).toSeq.sortBy(_._1)
			val oks = dataReader.numberOfEventsPerSecond((record: ChartRequestRecord) => record.executionEndDateNoMillis, Some(OK)).toSeq.sortBy(_._1)
			val kos = dataReader.numberOfEventsPerSecond((record: ChartRequestRecord) => record.executionEndDateNoMillis, Some(KO)).toSeq.sortBy(_._1)

			val allSeries = new Series[Long, Int]("All requests", all, List(BLUE))
			val kosSeries = new Series[Long, Int]("Failed requests", kos, List(RED))
			val oksSeries = new Series[Long, Int]("Succeeded requests", oks, List(GREEN))
			val pieRequestsSeries = new Series[String, Int]("Distribution", ("Success", count(oks)) :: ("Failures", count(kos)) :: Nil, List(GREEN, RED))

			componentLibrary.getTransactionsChartComponent(allSeries, kosSeries, oksSeries, pieRequestsSeries)
		}

		def responseTimeDistributionChartComponent = {
			val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100)
			val okDistributionSeries = new Series[Long, Int]("Success", okDistribution, List(BLUE))
			val koDistributionSeries = new Series[Long, Int]("Failure", koDistribution, List(RED))

			componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
		}

		def statisticsComponent = {
			val percent1 = configuration.chartingIndicatorsPercentile1 / 100.0
			val percent2 = configuration.chartingIndicatorsPercentile2 / 100.0

			val (globalPercentile1, globalPercentile2) = dataReader.percentiles(percent1, percent2)
			val (successPercentile1, successPercentile2) = dataReader.percentiles(percent1, percent2, Some(OK))
			val (failedPercentile1, failedPercentile2) = dataReader.percentiles(percent1, percent2, Some(KO))

			val globalMeanResponseTime = dataReader.meanResponseTime()
			val okMeanResponseTime = dataReader.meanResponseTime(Some(OK))
			val koMeanResponseTime = dataReader.meanResponseTime(Some(KO))
			val globalStandardDeviation = dataReader.responseTimeStandardDeviation()
			val okStandardDeviation = dataReader.responseTimeStandardDeviation(Some(OK))
			val koStandardDeviation = dataReader.responseTimeStandardDeviation(Some(KO))

			val numberOfRequestsStatistics = new Statistics("numberOfRequests", totalCount, okCount, koCount)
			val minResponseTimeStatistics = new Statistics("min", globalMinResponseTime, okMinResponseTime, koMinResponseTime)
			val maxResponseTimeStatistics = new Statistics("max", globalMaxResponseTime, okMaxResponseTime, koMaxResponseTime)
			val meanStatistics = new Statistics("mean", globalMeanResponseTime, okMeanResponseTime, koMeanResponseTime)
			val stdDeviationStatistics = new Statistics("stdDeviation", globalStandardDeviation, okStandardDeviation, koStandardDeviation)
			val percentiles1 = new Statistics("percentiles1", globalPercentile1, successPercentile1, failedPercentile1)
			val percentiles2 = new Statistics("percentiles2", globalPercentile2, successPercentile2, failedPercentile2)

			new StatisticsTextComponent(numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, meanStatistics, stdDeviationStatistics, percentiles1, percentiles2)
		}

		def indicatorChartComponent = {
			val indicatorsColumnData = dataReader.numberOfRequestInResponseTimeRange(configuration.chartingIndicatorsLowerBound, configuration.chartingIndicatorsHigherBound)
			val indicatorsPieData = indicatorsColumnData.map { case (name, count) => name -> count * 100 / totalCount }
			val indicatorsColumnSeries = new Series[String, Int](EMPTY, indicatorsColumnData, List(GREEN, YELLOW, ORANGE, RED))
			val indicatorsPieSeries = new Series[String, Int](EMPTY, indicatorsPieData, List(GREEN, YELLOW, ORANGE, RED))

			componentLibrary.getRequestDetailsIndicatorChartComponent(indicatorsColumnSeries, indicatorsPieSeries)
		}

		val template = new GlobalPageTemplate(
			statisticsComponent,
			indicatorChartComponent,
			activeSessionsChartComponent,
			responseTimeDistributionChartComponent,
			requestsChartComponent,
			transactionsChartComponent)

		new TemplateWriter(globalFile(runOn)).writeToFile(template.getOutput)
	}
}
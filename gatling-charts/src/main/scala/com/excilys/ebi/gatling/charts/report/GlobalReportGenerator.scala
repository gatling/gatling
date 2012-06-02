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

import scala.util.Sorting

import com.excilys.ebi.gatling.charts.component.{ StatisticsTextComponent, Statistics, ComponentLibrary }
import com.excilys.ebi.gatling.charts.config.ChartsFiles.globalFile
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.GlobalPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ toString, YELLOW, RED, PURPLE, PINK, ORANGE, LIME, LIGHT_RED, LIGHT_PURPLE, LIGHT_PINK, LIGHT_ORANGE, LIGHT_LIME, LIGHT_BLUE, GREEN, CYAN, BLUE }
import com.excilys.ebi.gatling.charts.util.StatisticsHelper.{ responseTimeStandardDeviation, responseTimePercentile, responseTimeDistribution, numberOfRequestsPerSecondAsList, numberOfRequestsPerSecond, numberOfRequestInResponseTimeRange, numberOfActiveSessionsPerSecond, minResponseTime, meanResponseTime, maxResponseTime, count }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ OK, KO }
import com.excilys.ebi.gatling.core.result.reader.{ DataReader, ChartRequestRecord }
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

class GlobalReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		val requests = dataReader.realChartRequestRecords
		val (successRequests, failedRequests) = requests.partition(_.requestStatus == OK)

		val numberOfRequests = requests.length
		val numberOfSuccessfulRequests = successRequests.length
		val numberOfFailedRequests = numberOfRequests - numberOfSuccessfulRequests

		val globalMinResponseTime = minResponseTime(requests)
		val globalMaxResponseTime = maxResponseTime(requests)

		def activeSessionsChartComponent = {
			val activeSessionsData = dataReader
				.scenarioNames
				.map { scenarioName => (scenarioName, dataReader.scenarioChartRequestRecordsGroupByExecutionStartDateInSeconds(scenarioName)) }
				.map { case (scenarioName, scenarioData) => scenarioName -> numberOfActiveSessionsPerSecond(scenarioData) }
				.reverse

			val activeSessionsSeries = activeSessionsData
				.zip(List(BLUE, GREEN, RED, YELLOW, CYAN, LIME, PURPLE, PINK, LIGHT_BLUE, LIGHT_ORANGE, LIGHT_RED, LIGHT_LIME, LIGHT_PURPLE, LIGHT_PINK))
				.map { case ((scenarioName, data), color) => new Series[Long, Int](scenarioName, data, List(color)) }

			componentLibrary.getActiveSessionsChartComponent(activeSessionsSeries)
		}

		def requestsChartComponent = {
			val requestsData = dataReader.realChartRequestRecordsGroupByExecutionStartDateInSeconds
			val succeededRequestsData = numberOfRequestsPerSecond(requestsData, OK)
			val failedRequestsData = numberOfRequestsPerSecond(requestsData, KO)

			val allRequestsSeries = new Series[Long, Int]("All requests", numberOfRequestsPerSecondAsList(requestsData), List(BLUE))
			val failedRequestsSeries = new Series[Long, Int]("Failed requests", failedRequestsData, List(RED))
			val succeededRequestsSeries = new Series[Long, Int]("Succeeded requests", succeededRequestsData, List(GREEN))
			val pieRequestsSeries = new Series[String, Int]("Repartition", ("Success", count(succeededRequestsData)) :: ("Failures", count(failedRequestsData)) :: Nil, List(GREEN, RED))

			componentLibrary.getRequestsChartComponent(allRequestsSeries, allRequestsSeries, succeededRequestsSeries, pieRequestsSeries)
		}

		def transactionsChartComponent = {
			val transactionsData = dataReader.realChartRequestRecordsGroupByExecutionEndDateInSeconds
			val failedTransactionsData = numberOfRequestsPerSecond(transactionsData, KO)
			val succeededTransactionsData = numberOfRequestsPerSecond(transactionsData, OK)

			val allTransactions = new Series[Long, Int]("All requests", numberOfRequestsPerSecondAsList(transactionsData), List(BLUE))
			val failedTransactions = new Series[Long, Int]("Failed requests", failedTransactionsData, List(RED))
			val succeededTransactions = new Series[Long, Int]("Succeeded requests", succeededTransactionsData, List(GREEN))
			val pieTransactionsSeries = new Series[String, Int]("Repartition", ("Success", count(succeededTransactionsData)) :: ("Failures", count(failedTransactionsData)) :: Nil, List(GREEN, RED))

			componentLibrary.getTransactionsChartComponent(allTransactions, failedTransactions, failedTransactions, pieTransactionsSeries)
		}

		def requestDetailsResponseTimeDistributionChartComponent = {
			val successDistribution = responseTimeDistribution(successRequests, globalMinResponseTime, globalMaxResponseTime, 100, numberOfRequests)
			val failedDistribution = responseTimeDistribution(failedRequests, globalMinResponseTime, globalMaxResponseTime, 100, numberOfRequests)
			val responseTimesSuccessDistributionSeries = new Series[Long, Int]("Success", successDistribution, List(BLUE))
			val responseTimesFailuresDistributionSeries = new Series[Long, Int]("Failure", failedDistribution, List(RED))

			componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(responseTimesSuccessDistributionSeries, responseTimesFailuresDistributionSeries)
		}

		def statisticsTextComponent = {
			val percent1 = configuration.chartingIndicatorsPercentile1 / 100.0
			val percent2 = configuration.chartingIndicatorsPercentile2 / 100.0

			def percentiles(requests: Seq[ChartRequestRecord]) = {
				implicit val ordering = Ordering.by((_: ChartRequestRecord).responseTime)
				val sortedRequests = Sorting.stableSort(requests)
				val percentile1 = responseTimePercentile(sortedRequests, percent1)
				val percentile2 = responseTimePercentile(sortedRequests, percent2)

				(percentile1, percentile2)
			}

			val (globalPercentile1, globalPercentile2) = percentiles(requests)
			val (successPercentile1, successPercentile2) = percentiles(successRequests)
			val (failedPercentile1, failedPercentile2) = percentiles(failedRequests)

			val numberOfRequestsStatistics = new Statistics("numberOfRequests", numberOfRequests, numberOfSuccessfulRequests, numberOfFailedRequests)
			val minResponseTimeStatistics = new Statistics("min", globalMinResponseTime, minResponseTime(successRequests), minResponseTime(failedRequests))
			val maxResponseTimeStatistics = new Statistics("max", globalMaxResponseTime, maxResponseTime(successRequests), maxResponseTime(failedRequests))
			val meanStatistics = new Statistics("mean", meanResponseTime(requests), meanResponseTime(successRequests), meanResponseTime(failedRequests))
			val stdDeviationStatistics = new Statistics("stdDeviation", responseTimeStandardDeviation(requests), responseTimeStandardDeviation(successRequests), responseTimeStandardDeviation(failedRequests))
			val percentiles1 = new Statistics("percentiles1", globalPercentile1, successPercentile1, failedPercentile1)
			val percentiles2 = new Statistics("percentiles2", globalPercentile2, successPercentile2, failedPercentile2)

			new StatisticsTextComponent(numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, meanStatistics, stdDeviationStatistics, percentiles1, percentiles2)
		}

		def requestDetailsIndicatorChartComponent = {
			val indicatorsColumnData = numberOfRequestInResponseTimeRange(requests, configuration.chartingIndicatorsLowerBound, configuration.chartingIndicatorsHigherBound)
			val indicatorsPieData = indicatorsColumnData.map { case (name, count) => name -> count * 100 / numberOfRequests }
			val indicatorsColumnSeries = new Series[String, Int](EMPTY, indicatorsColumnData, List(GREEN, YELLOW, ORANGE, RED))
			val indicatorsPieSeries = new Series[String, Int](EMPTY, indicatorsPieData, List(GREEN, YELLOW, ORANGE, RED))

			componentLibrary.getRequestDetailsIndicatorChartComponent(indicatorsColumnSeries, indicatorsPieSeries)
		}

		val template = new GlobalPageTemplate(
			statisticsTextComponent,
			requestDetailsIndicatorChartComponent,
			activeSessionsChartComponent,
			requestDetailsResponseTimeDistributionChartComponent,
			requestsChartComponent,
			transactionsChartComponent)

		new TemplateWriter(globalFile(runOn)).writeToFile(template.getOutput)
	}
}
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
import com.excilys.ebi.gatling.charts.util.StatisticsHelper.{ responseTimeStandardDeviation, responseTimePercentile, responseTimeDistribution, numberOfRequestsPerSecondAsList, numberOfRequestsPerSecond, numberOfRequestInResponseTimeRange, numberOfActiveSessionsPerSecond, minResponseTime, maxResponseTime, count, averageResponseTime }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ OK, KO }
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

class GlobalReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {

		// Active Sessions Series
		val activeSessionsSeries = {
			val activeSessionsData = dataReader
				.scenarioNames
				.map { scenarioName => (scenarioName, dataReader.scenarioRequestRecordsGroupByExecutionStartDateInSeconds(scenarioName)) }
				.map { case (scenarioName, scenarioData) => scenarioName -> numberOfActiveSessionsPerSecond(scenarioData) }
				.reverse

			activeSessionsData
				.zip(List(BLUE, GREEN, RED, YELLOW, CYAN, LIME, PURPLE, PINK, LIGHT_BLUE, LIGHT_ORANGE, LIGHT_RED, LIGHT_LIME, LIGHT_PURPLE, LIGHT_PINK))
				.map {
					case ((scenarioName, data), color) => new Series[Long, Int](scenarioName, data, List(color))
				}
		}

		// Requests Series
		val requestsData = dataReader.realRequestRecordsGroupByExecutionStartDateInSeconds
		val succeededRequestsData = numberOfRequestsPerSecond(requestsData, OK)
		val failedRequestsData = numberOfRequestsPerSecond(requestsData, KO)

		val allRequestsSeries = new Series[Long, Int]("All requests", numberOfRequestsPerSecondAsList(requestsData), List(BLUE))
		val failedRequestsSeries = new Series[Long, Int]("Failed requests", failedRequestsData, List(RED))
		val succeededRequestsSeries = new Series[Long, Int]("Succeeded requests", succeededRequestsData, List(GREEN))
		val pieRequestsSeries = new Series[String, Int]("Repartition", ("Success", count(succeededRequestsData)) :: ("Failures", count(failedRequestsData)) :: Nil, List(GREEN, RED))

		// Transactions Series
		val transactionsData = dataReader.realRequestRecordsGroupByExecutionEndDateInSeconds
		val failedTransactionsData = numberOfRequestsPerSecond(transactionsData, KO)
		val succeededTransactionsData = numberOfRequestsPerSecond(transactionsData, OK)

		val allTransactions = new Series[Long, Int]("All requests", numberOfRequestsPerSecondAsList(transactionsData), List(BLUE))
		val failedTransactions = new Series[Long, Int]("Failed requests", failedTransactionsData, List(RED))
		val succeededTransactions = new Series[Long, Int]("Succeeded requests", succeededTransactionsData, List(GREEN))
		val pieTransactionsSeries = new Series[String, Int]("Repartition", ("Success", count(succeededTransactionsData)) :: ("Failures", count(failedTransactionsData)) :: Nil, List(GREEN, RED))

		// Statistics
		val requests = dataReader.realRequestRecords
		val successRequests = requests.filter(_.requestStatus == OK)
		val failedRequests = requests.filter(_.requestStatus != OK)

		val numberOfRequests = requests.length
		val numberOfSuccessfulRequests = successRequests.length
		val numberOfFailedRequests = numberOfRequests - numberOfSuccessfulRequests

		val globalMinResponseTime = minResponseTime(requests)
		val globalMaxResponseTime = maxResponseTime(requests)

		// percentiles
		val successDistribution = responseTimeDistribution(successRequests, globalMinResponseTime, globalMaxResponseTime, 100, numberOfRequests)
		val failedDistribution = responseTimeDistribution(failedRequests, globalMinResponseTime, globalMaxResponseTime, 100, numberOfRequests)

		// Statistics
		val globalAverageResponseTime = averageResponseTime(requests)
		val successAverageResponseTime = averageResponseTime(successRequests)
		val failedAverageResponseTime = averageResponseTime(failedRequests)

		val sortedRequests = requests.sortBy(_.responseTime)
		val sortedSuccessRequests = successRequests.sortBy(_.responseTime)
		val sortedFailedRequests = successRequests.sortBy(_.responseTime)

		val global95Percentile = responseTimePercentile(sortedRequests, 0.95)
		val success95Percentile = responseTimePercentile(sortedRequests, 0.95)
		val failed95Percentile = responseTimePercentile(sortedFailedRequests, 0.95)
		val global99Percentile = responseTimePercentile(sortedRequests, 0.99)
		val success99Percentile = responseTimePercentile(sortedRequests, 0.99)
		val failed99Percentile = responseTimePercentile(sortedFailedRequests, 0.99)

		// Create series
		val responseTimesSuccessDistributionSeries = new Series[Long, Int]("Success", successDistribution, List(BLUE))
		val responseTimesFailuresDistributionSeries = new Series[Long, Int]("Failure", failedDistribution, List(RED))

		// Create Statistics
		val numberOfRequestsStatistics = new Statistics("numberOfRequests", numberOfRequests, numberOfSuccessfulRequests, numberOfFailedRequests)
		val minResponseTimeStatistics = new Statistics("min", globalMinResponseTime, minResponseTime(successRequests), minResponseTime(failedRequests))
		val maxResponseTimeStatistics = new Statistics("max", globalMaxResponseTime, maxResponseTime(successRequests), maxResponseTime(failedRequests))
		val averageStatistics = new Statistics("average", globalAverageResponseTime, successAverageResponseTime, failedAverageResponseTime)
		val stdDeviationStatistics = new Statistics("stdDeviation", responseTimeStandardDeviation(requests), responseTimeStandardDeviation(successRequests), responseTimeStandardDeviation(failedRequests))
		val percentiles95 = new Statistics("percentiles95", global95Percentile, success95Percentile, failed95Percentile)
		val percentiles99 = new Statistics("percentiles99", global99Percentile, success99Percentile, failed99Percentile)

		// Indicators Series
		val indicatorsColumnData = numberOfRequestInResponseTimeRange(requests, configuration.chartingIndicatorsLowerBound, configuration.chartingIndicatorsHigherBound)
		val indicatorsPieData = indicatorsColumnData.map { case (name, count) => name -> count * 100 / numberOfRequests }

		val indicatorsColumnSeries = new Series[String, Int](EMPTY, indicatorsColumnData, List(GREEN, YELLOW, ORANGE, RED))
		val indicatorsPieSeries = new Series[String, Int](EMPTY, indicatorsPieData, List(GREEN, YELLOW, ORANGE, RED))

		val template = new GlobalPageTemplate(
			new StatisticsTextComponent(numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, averageStatistics, stdDeviationStatistics, percentiles95, percentiles99),
			componentLibrary.getRequestDetailsIndicatorChartComponent(indicatorsColumnSeries, indicatorsPieSeries),
			componentLibrary.getActiveSessionsChartComponent(activeSessionsSeries),
			componentLibrary.getRequestsChartComponent(allRequestsSeries, failedRequestsSeries, succeededRequestsSeries, pieRequestsSeries),
			componentLibrary.getTransactionsChartComponent(allTransactions, failedTransactions, succeededTransactions, pieTransactionsSeries))

		new TemplateWriter(globalFile(runOn)).writeToFile(template.getOutput)

	}
}
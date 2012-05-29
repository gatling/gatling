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
import com.excilys.ebi.gatling.charts.config.ChartsFiles.requestFile
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.RequestDetailsPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ toString, YELLOW, TRANSLUCID_RED, TRANSLUCID_BLUE, RED, ORANGE, GREEN, BLUE }
import com.excilys.ebi.gatling.charts.util.StatisticsHelper.{ responseTimeStandardDeviation, responseTimePercentile, responseTimeDistribution, responseTimeByMillisecondAsList, respTimeAgainstNbOfReqPerSecond, numberOfRequestsPerSecond, numberOfRequestInResponseTimeRange, minResponseTime, maxResponseTime, latencyByMillisecondAsList, averageResponseTime }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ OK, KO }
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

class RequestDetailsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		dataReader.requestNames.foreach { requestName =>
			val requests = dataReader.requestRecords(requestName)
			val successRequests = requests.filter(_.requestStatus == OK)
			val failedRequests = requests.filter(_.requestStatus != OK)

			val numberOfRequests = requests.length
			val numberOfSuccessfulRequests = successRequests.length
			val numberOfFailedRequests = numberOfRequests - numberOfSuccessfulRequests

			val globalMinResponseTime = minResponseTime(requests)
			val globalMaxResponseTime = maxResponseTime(requests)

			val dataMillis = dataReader.requestRecordsGroupByExecutionStartDate(requestName)
			val dataSeconds = dataReader.requestRecordsGroupByExecutionStartDateInSeconds(requestName)

			// Get Data
			val responseTimesSuccessData = responseTimeByMillisecondAsList(dataMillis, OK)
			val responseTimesFailuresData = responseTimeByMillisecondAsList(dataMillis, KO)
			val latencySuccessData = latencyByMillisecondAsList(dataMillis, OK)
			val latencyFailuresData = latencyByMillisecondAsList(dataMillis, KO)
			val indicatorsColumnData = numberOfRequestInResponseTimeRange(requests, configuration.chartingIndicatorsLowerBound, configuration.chartingIndicatorsHigherBound)
			val indicatorsPieData = indicatorsColumnData.map { case (name, count) => name -> count * 100 / numberOfRequests }
			val requestsPerSecond = numberOfRequestsPerSecond(dataReader.requestRecordsGroupByExecutionStartDateInSeconds)
			val scatterPlotSuccessData = respTimeAgainstNbOfReqPerSecond(requestsPerSecond, dataSeconds, OK)
			val scatterPlotFailuresData = respTimeAgainstNbOfReqPerSecond(requestsPerSecond, dataSeconds, KO)

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
			val responseTimesSuccessSeries = new Series[Long, Long]("Response Time (success)", responseTimesSuccessData, List(BLUE))
			val responseTimesFailuresSeries = new Series[Long, Long]("Response Time (failure)", responseTimesFailuresData, List(RED))
			val responseTimesSuccessDistributionSeries = new Series[Long, Int]("Success", successDistribution, List(BLUE))
			val responseTimesFailuresDistributionSeries = new Series[Long, Int]("Failure", failedDistribution, List(RED))
			val latencySuccessSeries = new Series[Long, Long]("Latency (success)", latencySuccessData, List(BLUE))
			val latencyFailuresSeries = new Series[Long, Long]("Latency (failure)", latencyFailuresData, List(RED))
			val indicatorsColumnSeries = new Series[String, Int](EMPTY, indicatorsColumnData, List(GREEN, YELLOW, ORANGE, RED))
			val indicatorsPieSeries = new Series[String, Int](EMPTY, indicatorsPieData, List(GREEN, YELLOW, ORANGE, RED))
			val scatterPlotSuccessSeries = new Series[Int, Long]("Successes", scatterPlotSuccessData, List(TRANSLUCID_BLUE))
			val scatterPlotFailuresSeries = new Series[Int, Long]("Failures", scatterPlotFailuresData, List(TRANSLUCID_RED))

			val numberOfRequestsStatistics = new Statistics("numberOfRequests", numberOfRequests, numberOfSuccessfulRequests, numberOfFailedRequests)
			val minResponseTimeStatistics = new Statistics("min", globalMinResponseTime, minResponseTime(successRequests), minResponseTime(failedRequests))
			val maxResponseTimeStatistics = new Statistics("max", globalMaxResponseTime, maxResponseTime(successRequests), maxResponseTime(failedRequests))
			val averageStatistics = new Statistics("average", globalAverageResponseTime, successAverageResponseTime, failedAverageResponseTime)
			val stdDeviationStatistics = new Statistics("stdDeviation", responseTimeStandardDeviation(requests), responseTimeStandardDeviation(successRequests), responseTimeStandardDeviation(failedRequests))
			val percentiles95 = new Statistics("percentiles95", global95Percentile, success95Percentile, failed95Percentile)
			val percentiles99 = new Statistics("percentiles99", global99Percentile, success99Percentile, failed99Percentile)

			// Create template
			val template =
				new RequestDetailsPageTemplate(requestName.substring(8),
					componentLibrary.getRequestDetailsResponseTimeChartComponent(responseTimesSuccessSeries, responseTimesFailuresSeries),
					componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(responseTimesSuccessDistributionSeries, responseTimesFailuresDistributionSeries),
					componentLibrary.getRequestDetailsLatencyChartComponent(latencySuccessSeries, latencyFailuresSeries),
					new StatisticsTextComponent(numberOfRequestsStatistics, minResponseTimeStatistics, maxResponseTimeStatistics, averageStatistics, stdDeviationStatistics, percentiles95, percentiles99),
					componentLibrary.getRequestDetailsScatterChartComponent(scatterPlotSuccessSeries, scatterPlotFailuresSeries),
					componentLibrary.getRequestDetailsIndicatorChartComponent(indicatorsColumnSeries, indicatorsPieSeries))

			// Write template result to file
			new TemplateWriter(requestFile(runOn, requestName)).writeToFile(template.getOutput)
		}
	}
}
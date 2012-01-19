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

import org.joda.time.DateTime

import com.excilys.ebi.gatling.charts.component.{ StatisticsTextComponent, ComponentLibrary }
import com.excilys.ebi.gatling.charts.computer.Computer.{ responseTimeStandardDeviation, responseTimeByMillisecondAsList, latencyByMillisecondAsList, respTimeAgainstNbOfReqPerSecond, numberOfSuccesses, numberOfRequestsPerSecond, numberOfRequestInResponseTimeRange, minResponseTime, maxResponseTime, averageResponseTime }
import com.excilys.ebi.gatling.charts.config.ChartsFiles.requestFile
import com.excilys.ebi.gatling.charts.loader.DataLoader
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.series.SharedSeries
import com.excilys.ebi.gatling.charts.template.RequestDetailsPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ toString, YELLOW, TRANSLUCID_RED, TRANSLUCID_BLUE, RED, ORANGE, GREEN, BLUE }
import com.excilys.ebi.gatling.charts.writer.TemplateWriter
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfig.{ CONFIG_CHARTING_INDICATORS_LOWER_BOUND, CONFIG_CHARTING_INDICATORS_HIGHER_BOUND }
import com.excilys.ebi.gatling.core.result.message.ResultStatus.{ OK, KO }
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

class RequestDetailsReportGenerator(runOn: String, dataLoader: DataLoader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataLoader, componentLibrary) {
	def generate = {

		dataLoader.requestNames.foreach { requestName =>
			val dataList = dataLoader.requestData(requestName)

			if (requestName != END_OF_SCENARIO && requestName != START_OF_SCENARIO) {
				val dataMillis = dataLoader.requestDataIndexedBySendDate(requestName)
				val dataSeconds = dataLoader.requestDataIndexedBySendDateWithoutMillis(requestName)

				// Get Data
				val responseTimesSuccessData = responseTimeByMillisecondAsList(dataMillis, OK)
				val responseTimesFailuresData = responseTimeByMillisecondAsList(dataMillis, KO)
				val latencySuccessData = latencyByMillisecondAsList(dataMillis, OK)
				val latencyFailuresData = latencyByMillisecondAsList(dataMillis, KO)
				val indicatorsColumnData = numberOfRequestInResponseTimeRange(dataList, CONFIG_CHARTING_INDICATORS_LOWER_BOUND, CONFIG_CHARTING_INDICATORS_HIGHER_BOUND)
				val indicatorsPieData = {
					val numberOfRequests = dataList.size
					indicatorsColumnData.map { entry => entry._1 -> (entry._2 / numberOfRequests.toDouble * 100).toInt }
				}
				val requestsPerSecond = numberOfRequestsPerSecond(dataLoader.dataIndexedBySendDateWithoutMillis)
				val scatterPlotSuccessData = respTimeAgainstNbOfReqPerSecond(requestsPerSecond, dataSeconds, OK)
				val scatterPlotFailuresData = respTimeAgainstNbOfReqPerSecond(requestsPerSecond, dataSeconds, KO)
				
				// Statistics
				val numberOfRequests = dataList.length
				val numberOfSuccessfulRequests = numberOfSuccesses(dataList)
				val numberOfFailedRequests = numberOfRequests - numberOfSuccessfulRequests
				val minRespTime = minResponseTime(dataList)
				val maxRespTime = maxResponseTime(dataList)
				val avgRespTime = averageResponseTime(dataList)
				val respTimeStdDeviation = responseTimeStandardDeviation(dataList)

				// Create series
				val responseTimesSuccessSeries = new Series[Long, Int]("Response Time (success)", responseTimesSuccessData, List(BLUE))
				val responseTimesFailuresSeries = new Series[Long, Int]("Response Time (failure)", responseTimesFailuresData, List(RED))
				val latencySuccessSeries = new Series[Long, Int]("Latency (success)", latencySuccessData, List(BLUE))
				val latencyFailuresSeries = new Series[Long, Int]("Latency (failure)", latencyFailuresData, List(RED))
				val indicatorsColumnSeries = new Series[String, Int](EMPTY, indicatorsColumnData, List(GREEN, YELLOW, ORANGE, RED))
				val indicatorsPieSeries = new Series[String, Int](EMPTY, indicatorsPieData, List(GREEN, YELLOW, ORANGE, RED))
				val scatterPlotSuccessSeries = new Series[Int, Long]("Successes", scatterPlotSuccessData, List(TRANSLUCID_BLUE))
				val scatterPlotFailuresSeries = new Series[Int, Long]("Failures", scatterPlotFailuresData, List(TRANSLUCID_RED))

				// Create template
				val template =
					new RequestDetailsPageTemplate(requestName.substring(8),
						componentLibrary.getRequestDetailsResponseTimeChartComponent(responseTimesSuccessSeries, responseTimesFailuresSeries, SharedSeries.getAllActiveSessionsSeries),
						componentLibrary.getRequestDetailsLatencyChartComponent(latencySuccessSeries, latencyFailuresSeries, SharedSeries.getAllActiveSessionsSeries),
						new StatisticsTextComponent(numberOfRequests, numberOfSuccessfulRequests, numberOfFailedRequests, minRespTime, maxRespTime, avgRespTime, respTimeStdDeviation),
						componentLibrary.getRequestDetailsScatterChartComponent(scatterPlotSuccessSeries, scatterPlotFailuresSeries),
						componentLibrary.getRequestDetailsIndicatorChartComponent(indicatorsColumnSeries, indicatorsPieSeries))

				// Write template result to file
				new TemplateWriter(requestFile(runOn, requestName)).writeToFile(template.getOutput)
			}
		}
	}
}
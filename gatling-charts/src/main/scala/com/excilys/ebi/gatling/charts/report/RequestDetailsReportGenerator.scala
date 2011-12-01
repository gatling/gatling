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
package com.excilys.ebi.gatling.charts.report
import org.joda.time.DateTime
import com.excilys.ebi.gatling.charts.config.ChartsFiles._
import com.excilys.ebi.gatling.charts.component.ComponentLibrary
import com.excilys.ebi.gatling.charts.component.StatisticsTextComponent
import com.excilys.ebi.gatling.charts.computer.Computer.averageResponseTime
import com.excilys.ebi.gatling.charts.computer.Computer.maxResponseTime
import com.excilys.ebi.gatling.charts.computer.Computer.minResponseTime
import com.excilys.ebi.gatling.charts.computer.Computer.numberOfRequestInResponseTimeRange
import com.excilys.ebi.gatling.charts.computer.Computer.numberOfRequestsPerSecond
import com.excilys.ebi.gatling.charts.computer.Computer.respTimeAgainstNbOfReqPerSecond
import com.excilys.ebi.gatling.charts.computer.Computer.responseTimeByMillisecondAsList
import com.excilys.ebi.gatling.charts.computer.Computer._
import com.excilys.ebi.gatling.charts.loader.DataLoader
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.RequestDetailsPageTemplate
import com.excilys.ebi.gatling.charts.writer.TemplateWriter
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_CHARTING_INDICATORS_HIGHER_BOUND
import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_CHARTING_INDICATORS_LOWER_BOUND
import com.excilys.ebi.gatling.core.util.FileHelper.HTML_EXTENSION
import com.excilys.ebi.gatling.core.util.FileHelper.formatToFilename
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.charts.series.SharedSeries
import com.excilys.ebi.gatling.core.result.message.ResultStatus._

class RequestDetailsReportGenerator(runOn: String, dataLoader: DataLoader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataLoader, componentLibrary) {
	def generate = {

		dataLoader.requestNames.foreach { requestName =>
			val dataList = dataLoader.dataIndexedByRequestName(requestName)

			if (requestName != END_OF_SCENARIO && requestName != START_OF_SCENARIO) {
				val dataMillis = dataLoader.dataIndexedByRequestNameAndDateInMilliseconds(requestName)
				val dataSeconds = dataLoader.dataIndexedByRequestNameAndDateInSeconds(requestName)

				// Get Data
				val responseTimesData = responseTimeByMillisecondAsList(dataMillis)
				val indicatorsColumnData = numberOfRequestInResponseTimeRange(dataList, CONFIG_CHARTING_INDICATORS_LOWER_BOUND, CONFIG_CHARTING_INDICATORS_HIGHER_BOUND)
				val indicatorsPieData = {
					val numberOfRequests = dataList.size
					indicatorsColumnData.map { entry => entry._1 -> (entry._2 / numberOfRequests.toDouble * 100).toInt }
				}
				val scatterPlotSuccessData = respTimeAgainstNbOfReqPerSecond(numberOfRequestsPerSecond(dataLoader.dataIndexedByDateInSeconds), dataSeconds, OK)
				val scatterPlotFailuresData = respTimeAgainstNbOfReqPerSecond(numberOfRequestsPerSecond(dataLoader.dataIndexedByDateInSeconds), dataSeconds, KO)

				// Statistics
				val numberOfRequests = dataList.length
				val numberOfSuccessfulRequests = numberOfSuccesses(dataList)
				val numberOfFailedRequests = numberOfRequests - numberOfSuccessfulRequests
				val minRespTime = minResponseTime(dataList)
				val maxRespTime = maxResponseTime(dataList)
				val avgRespTime = averageResponseTime(dataList)
				val respTimeStdDeviation = responseTimeStandardDeviation(dataList)

				// Create series
				val responseTimesSeries = new Series[DateTime, Int]("Response Time", responseTimesData)
				val indicatorsColumnSeries = new Series[String, Int](EMPTY, indicatorsColumnData)
				val indicatorsPieSeries = new Series[String, Int](EMPTY, indicatorsPieData)
				val scatterPlotSuccessSeries = new Series[Int, Int]("Successes", scatterPlotSuccessData)
				val scatterPlotFailuresSeries = new Series[Int, Int]("Failures", scatterPlotFailuresData)

				// Create template
				val template =
					new RequestDetailsPageTemplate(requestName.substring(8),
						componentLibrary.getRequestDetailsResponseTimeChartComponent(responseTimesSeries, SharedSeries.getAllActiveSessionsSeries),
						new StatisticsTextComponent(numberOfRequests, numberOfSuccessfulRequests, numberOfFailedRequests, minRespTime, maxRespTime, avgRespTime, respTimeStdDeviation),
						componentLibrary.getRequestDetailsScatterChartComponent(scatterPlotSuccessSeries, scatterPlotFailuresSeries),
						componentLibrary.getRequestDetailsIndicatorChartComponent(indicatorsColumnSeries, indicatorsPieSeries))

				// Write template result to file
				new TemplateWriter(requestFile(runOn, requestName)).writeToFile(template.getOutput)
			}
		}
	}
}
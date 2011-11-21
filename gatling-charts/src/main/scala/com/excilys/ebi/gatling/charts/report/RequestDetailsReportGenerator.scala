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
import com.excilys.ebi.gatling.charts.component.ComponentLibrary
import com.excilys.ebi.gatling.charts.component.StatisticsTextComponent
import com.excilys.ebi.gatling.charts.computer.Computer.averageResponseTime
import com.excilys.ebi.gatling.charts.computer.Computer.maxResponseTime
import com.excilys.ebi.gatling.charts.computer.Computer.minResponseTime
import com.excilys.ebi.gatling.charts.computer.Computer.numberOfRequestInResponseTimeRange
import com.excilys.ebi.gatling.charts.computer.Computer.numberOfRequestsPerSecond
import com.excilys.ebi.gatling.charts.computer.Computer.respTimeAgainstNbOfReqPerSecond
import com.excilys.ebi.gatling.charts.computer.Computer.responseTimeByMillisecondAsList
import com.excilys.ebi.gatling.charts.computer.Computer.responseTimeStandardDeviation
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

class RequestDetailsReportGenerator(runOn: String, dataLoader: DataLoader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataLoader, componentLibrary) {
	def generate = {

		val dataIndexedByRequestNameAndMillis = dataLoader.dataIndexedByRequestNameAndDateInMilliseconds
		val dataIndexedByRequestNameAndSeconds = dataLoader.dataIndexedByRequestNameAndDateInSeconds

		dataLoader.dataIndexedByRequestName.foreach { entry =>
			val (requestName, dataList) = entry

			if (requestName != END_OF_SCENARIO && requestName != START_OF_SCENARIO) {
				val dataMillis = dataIndexedByRequestNameAndMillis.getOrElse(requestName, throw new IllegalArgumentException("Data Not Indexed correctly !"))
				val dataSeconds = dataIndexedByRequestNameAndSeconds.getOrElse(requestName, throw new IllegalArgumentException("Data Not Indexed correctly !"))

				// Get Data
				val responseTimesData = responseTimeByMillisecondAsList(dataMillis)
				val indicatorsColumnData = numberOfRequestInResponseTimeRange(dataList, CONFIG_CHARTING_INDICATORS_LOWER_BOUND, CONFIG_CHARTING_INDICATORS_HIGHER_BOUND) // FIXME make boundaries configurable
				val indicatorsPieData = {
					val numberOfRequests = dataList.size
					indicatorsColumnData.map { entry => entry._1 -> (entry._2 / numberOfRequests.toDouble * 100).toInt }
				}
				val scatterPlotData = respTimeAgainstNbOfReqPerSecond(numberOfRequestsPerSecond(dataLoader.dataIndexedByDateInSeconds), dataSeconds)

				// Statistics
				val numberOfRequests = dataList.length
				val minRespTime = minResponseTime(dataList)
				val maxRespTime = maxResponseTime(dataList)
				val avgRespTime = averageResponseTime(dataList)
				val respTimeStdDeviation = responseTimeStandardDeviation(dataList)

				// Create series
				val responseTimesSeries = new Series[DateTime, Int]("Response Time", responseTimesData)
				val indicatorsColumnSeries = new Series[String, Int](EMPTY, indicatorsColumnData)
				val indicatorsPieSeries = new Series[String, Int](EMPTY, indicatorsPieData)
				val scatterPlotSeries = new Series[Int, Int](EMPTY, scatterPlotData)

				// Create template
				val template =
					new RequestDetailsPageTemplate(requestName.substring(8),
						componentLibrary.getRequestDetailsResponseTimeChartComponent(responseTimesSeries, SharedSeries.getAllActiveSessionsSeries),
						new StatisticsTextComponent(numberOfRequests, minRespTime, maxRespTime, avgRespTime, respTimeStdDeviation),
						componentLibrary.getRequestDetailsScatterChartComponent(scatterPlotSeries),
						componentLibrary.getRequestDetailsIndicatorChartComponent(indicatorsColumnSeries, indicatorsPieSeries))

				// Write template result to file
				new TemplateWriter(runOn, formatToFilename(requestName) + HTML_EXTENSION).writeToFile(template.getOutput)
			}
		}

	}
}
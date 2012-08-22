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

import com.excilys.ebi.gatling.charts.component.{ StatisticsTextComponent, ComponentLibrary }
import com.excilys.ebi.gatling.charts.config.ChartsFiles.requestFile
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.RequestDetailsPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ toString, TRANSLUCID_RED, TRANSLUCID_BLUE, RED, BLUE }
import com.excilys.ebi.gatling.charts.util.StatisticsHelper.{ responseTimeOverTime, respTimeAgainstNbOfReqPerSecond, latencyOverTime }
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ OK, KO }
import com.excilys.ebi.gatling.core.result.reader.{ DataReader, ChartRequestRecord }

class RequestDetailsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		dataReader.requestNames.foreach { requestName =>
			val dataMillis = dataReader.requestRecordsGroupByExecutionStartDate(requestName)

			def responseTimeChartComponent = {
				val responseTimesSuccessData = responseTimeOverTime(dataMillis, OK)
				val responseTimesFailuresData = responseTimeOverTime(dataMillis, KO)
				val responseTimesSuccessSeries = new Series[Long, Long]("Response Time (success)", responseTimesSuccessData, List(BLUE))
				val responseTimesFailuresSeries = new Series[Long, Long]("Response Time (failure)", responseTimesFailuresData, List(RED))

				componentLibrary.getRequestDetailsResponseTimeChartComponent(responseTimesSuccessSeries, responseTimesFailuresSeries)
			}

			def responseTimeDistributionChartComponent = {
				val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100, Some(requestName))
				val okDistributionSeries = new Series[Long, Long]("Success", okDistribution, List(BLUE))
				val koDistributionSeries = new Series[Long, Long]("Failure", koDistribution, List(RED))

				componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
			}

			def latencyChartComponent = {
				val latencySuccessData = latencyOverTime(dataMillis, OK)
				val latencyFailuresData = latencyOverTime(dataMillis, KO)

				val latencySuccessSeries = new Series[Long, Long]("Latency (success)", latencySuccessData, List(BLUE))
				val latencyFailuresSeries = new Series[Long, Long]("Latency (failure)", latencyFailuresData, List(RED))

				componentLibrary.getRequestDetailsLatencyChartComponent(latencySuccessSeries, latencyFailuresSeries)
			}

			def statisticsComponent = new StatisticsTextComponent

			def scatterChartComponent = {
				val all = dataReader.numberOfEventsPerSecond((record: ChartRequestRecord) => record.executionStartDateNoMillis)
				val dataSeconds = dataReader.requestRecordsGroupByExecutionStartDate(requestName)
				val scatterPlotSuccessData = respTimeAgainstNbOfReqPerSecond(all, dataSeconds, OK)
				val scatterPlotFailuresData = respTimeAgainstNbOfReqPerSecond(all, dataSeconds, KO)
				val scatterPlotSuccessSeries = new Series[Long, Long]("Successes", scatterPlotSuccessData, List(TRANSLUCID_BLUE))
				val scatterPlotFailuresSeries = new Series[Long, Long]("Failures", scatterPlotFailuresData, List(TRANSLUCID_RED))

				componentLibrary.getRequestDetailsScatterChartComponent(scatterPlotSuccessSeries, scatterPlotFailuresSeries)
			}

			def indicatorChartComponent = componentLibrary.getRequestDetailsIndicatorChartComponent

			// Create template
			val template =
				new RequestDetailsPageTemplate(requestName,
					responseTimeChartComponent,
					responseTimeDistributionChartComponent,
					latencyChartComponent,
					statisticsComponent,
					scatterChartComponent,
					indicatorChartComponent)

			// Write template result to file
			new TemplateWriter(requestFile(runOn, requestName)).writeToFile(template.getOutput)
		}
	}
}
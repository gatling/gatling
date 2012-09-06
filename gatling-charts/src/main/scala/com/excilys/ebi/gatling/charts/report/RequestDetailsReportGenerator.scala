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

import com.excilys.ebi.gatling.charts.component.{ Component, ComponentLibrary, StatisticsTextComponent }
import com.excilys.ebi.gatling.charts.config.ChartsFiles.requestFile
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.RequestDetailsPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ BLUE, RED, TRANSLUCID_BLUE, TRANSLUCID_RED, toString }
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ KO, OK }
import com.excilys.ebi.gatling.core.result.reader.DataReader

class RequestDetailsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		dataReader.requestNames.foreach { requestName =>

			def responseTimeChartComponent: Component = {
				val responseTimesSuccessData = dataReader.responseTimeGroupByExecutionStartDate(OK, requestName)
				val responseTimesFailuresData = dataReader.responseTimeGroupByExecutionStartDate(KO, requestName)
				val responseTimesSuccessSeries = new Series[Long, (Long, Long)]("Response Time (success)", responseTimesSuccessData, List(BLUE))
				val responseTimesFailuresSeries = new Series[Long, (Long, Long)]("Response Time (failure)", responseTimesFailuresData, List(RED))

				componentLibrary.getRequestDetailsResponseTimeChartComponent(responseTimesSuccessSeries, responseTimesFailuresSeries)
			}

			def responseTimeDistributionChartComponent: Component = {
				val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100, Some(requestName))
				val okDistributionSeries = new Series[Long, Long]("Success", okDistribution, List(BLUE))
				val koDistributionSeries = new Series[Long, Long]("Failure", koDistribution, List(RED))

				componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
			}

			def latencyChartComponent: Component = {
				val latencySuccessData = dataReader.latencyGroupByExecutionStartDate(OK, requestName)
				val latencyFailuresData = dataReader.latencyGroupByExecutionStartDate(KO, requestName)

				val latencySuccessSeries = new Series[Long, (Long, Long)]("Latency (success)", latencySuccessData, List(BLUE))
				val latencyFailuresSeries = new Series[Long, (Long, Long)]("Latency (failure)", latencyFailuresData, List(RED))

				componentLibrary.getRequestDetailsLatencyChartComponent(latencySuccessSeries, latencyFailuresSeries)
			}

			def statisticsComponent: Component = new StatisticsTextComponent

			def scatterChartComponent: Component = {
				val scatterPlotSuccessData = dataReader.requestAgainstResponseTime(OK, requestName)
				val scatterPlotFailuresData = dataReader.requestAgainstResponseTime(KO, requestName)
				val scatterPlotSuccessSeries = new Series[Long, Long]("Successes", scatterPlotSuccessData, List(TRANSLUCID_BLUE))
				val scatterPlotFailuresSeries = new Series[Long, Long]("Failures", scatterPlotFailuresData, List(TRANSLUCID_RED))

				componentLibrary.getRequestDetailsScatterChartComponent(scatterPlotSuccessSeries, scatterPlotFailuresSeries)
			}

			def indicatorChartComponent: Component = componentLibrary.getRequestDetailsIndicatorChartComponent

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

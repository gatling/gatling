/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
import com.excilys.ebi.gatling.charts.template.RequestDetailsPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ BLUE, RED, TRANSLUCID_BLUE, TRANSLUCID_RED }
import com.excilys.ebi.gatling.core.result.{ Group, IntRangeVsTimePlot, IntVsTimePlot, RequestPath, Series }
import com.excilys.ebi.gatling.core.result.message.{ KO, OK }
import com.excilys.ebi.gatling.core.result.reader.DataReader

class RequestDetailsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		def generateDetailPage(path: String, requestName: Option[String], group: Option[Group]) {
			def responseTimeChartComponent: Component = {
				val responseTimesSuccessData = dataReader.responseTimeGroupByExecutionStartDate(OK, requestName, group)
				val responseTimesFailuresData = dataReader.responseTimeGroupByExecutionStartDate(KO, requestName, group)
				val responseTimesSuccessSeries = new Series[IntRangeVsTimePlot]("Response Time (success)", responseTimesSuccessData, List(BLUE))
				val responseTimesFailuresSeries = new Series[IntRangeVsTimePlot]("Response Time (failure)", responseTimesFailuresData, List(RED))

				componentLibrary.getRequestDetailsResponseTimeChartComponent(dataReader.runStart, responseTimesSuccessSeries, responseTimesFailuresSeries)
			}

			def responseTimeDistributionChartComponent: Component = {
				val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100, requestName, group)
				val okDistributionSeries = new Series[IntVsTimePlot]("Success", okDistribution, List(BLUE))
				val koDistributionSeries = new Series[IntVsTimePlot]("Failure", koDistribution, List(RED))

				componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
			}

			def latencyChartComponent: Component = {
				val latencySuccessData = dataReader.latencyGroupByExecutionStartDate(OK, requestName, group)
				val latencyFailuresData = dataReader.latencyGroupByExecutionStartDate(KO, requestName, group)

				val latencySuccessSeries = new Series[IntRangeVsTimePlot]("Latency (success)", latencySuccessData, List(BLUE))
				val latencyFailuresSeries = new Series[IntRangeVsTimePlot]("Latency (failure)", latencyFailuresData, List(RED))

				componentLibrary.getRequestDetailsLatencyChartComponent(dataReader.runStart, latencySuccessSeries, latencyFailuresSeries)
			}

			def statisticsComponent: Component = new StatisticsTextComponent

			def scatterChartComponent: Component = {
				val scatterPlotSuccessData = dataReader.responseTimeAgainstGlobalNumberOfRequestsPerSec(OK, requestName, group)
				val scatterPlotFailuresData = dataReader.responseTimeAgainstGlobalNumberOfRequestsPerSec(KO, requestName, group)
				val scatterPlotSuccessSeries = new Series[IntVsTimePlot]("Successes", scatterPlotSuccessData, List(TRANSLUCID_BLUE))
				val scatterPlotFailuresSeries = new Series[IntVsTimePlot]("Failures", scatterPlotFailuresData, List(TRANSLUCID_RED))

				componentLibrary.getRequestDetailsScatterChartComponent(scatterPlotSuccessSeries, scatterPlotFailuresSeries)
			}

			def indicatorChartComponent: Component = componentLibrary.getRequestDetailsIndicatorChartComponent

			// Create template
			val template =
				new RequestDetailsPageTemplate(path,
					requestName,
					group,
					responseTimeChartComponent,
					responseTimeDistributionChartComponent,
					latencyChartComponent,
					statisticsComponent,
					scatterChartComponent,
					indicatorChartComponent)

			// Write template result to file
			new TemplateWriter(requestFile(runOn, path)).writeToFile(template.getOutput)
		}

		dataReader.groupsAndRequests.foreach {
			case (group, Some(request)) => generateDetailPage(RequestPath.path(request, group), Some(request), group)
			case _ => {}
		}
	}
}

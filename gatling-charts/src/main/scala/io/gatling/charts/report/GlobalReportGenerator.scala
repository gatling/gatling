/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.charts.report

import io.gatling.charts.component.{ Component, ComponentLibrary, ErrorTableComponent, StatisticsTableComponent }
import io.gatling.charts.config.ChartsFiles.globalFile
import io.gatling.charts.template.GlobalPageTemplate
import io.gatling.charts.util.Colors._
import io.gatling.core.result.{ IntVsTimePlot, PieSlice, Series }
import io.gatling.core.result.message.{ KO, OK }
import io.gatling.core.result.reader.DataReader

class GlobalReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		def activeSessionsChartComponent = {
			val activeSessionsSeries: Seq[Series[IntVsTimePlot]] = dataReader
				.scenarioNames
				.map { scenarioName => scenarioName -> dataReader.numberOfActiveSessionsPerSecond(Some(scenarioName)) }
				.reverse
				.zip(List(BLUE, GREEN, RED, YELLOW, CYAN, LIME, PURPLE, PINK, LIGHT_BLUE, LIGHT_ORANGE, LIGHT_RED, LIGHT_LIME, LIGHT_PURPLE, LIGHT_PINK))
				.map { case ((scenarioName, data), color) => new Series[IntVsTimePlot](scenarioName, data, List(color)) }

			componentLibrary.getActiveSessionsChartComponent(dataReader.runStart, activeSessionsSeries)
		}

		def requestsChartComponent: Component = {
			val all = dataReader.numberOfRequestsPerSecond().sortBy(_.time)
			val oks = dataReader.numberOfRequestsPerSecond(Some(OK)).sortBy(_.time)
			val kos = dataReader.numberOfRequestsPerSecond(Some(KO)).sortBy(_.time)

			val allSeries = new Series[IntVsTimePlot]("All requests", all, List(BLUE))
			val kosSeries = new Series[IntVsTimePlot]("Failed requests", kos, List(RED))
			val oksSeries = new Series[IntVsTimePlot]("Succeeded requests", oks, List(GREEN))
			val pieRequestsSeries = new Series[PieSlice]("Distribution", PieSlice("Success", count(oks)) :: PieSlice("Failures", count(kos)) :: Nil, List(GREEN, RED))

			componentLibrary.getRequestsChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
		}

		def responsesChartComponent: Component = {
			val all = dataReader.numberOfResponsesPerSecond().sortBy(_.time)
			val oks = dataReader.numberOfResponsesPerSecond(Some(OK)).sortBy(_.time)
			val kos = dataReader.numberOfResponsesPerSecond(Some(KO)).sortBy(_.time)

			val allSeries = new Series[IntVsTimePlot]("All responses", all, List(BLUE))
			val kosSeries = new Series[IntVsTimePlot]("Failed responses", kos, List(RED))
			val oksSeries = new Series[IntVsTimePlot]("Succeeded responses", oks, List(GREEN))
			val pieRequestsSeries = new Series[PieSlice]("Distribution", PieSlice("Success", count(oks)) :: PieSlice("Failures", count(kos)) :: Nil, List(GREEN, RED))

			componentLibrary.getResponsesChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
		}

		def responseTimeDistributionChartComponent: Component = {
			val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100)
			val okDistributionSeries = new Series[IntVsTimePlot]("Success", okDistribution, List(BLUE))
			val koDistributionSeries = new Series[IntVsTimePlot]("Failure", koDistribution, List(RED))

			componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
		}

		val template = new GlobalPageTemplate(
			componentLibrary.getNumberOfRequestsChartComponent,
			componentLibrary.getRequestDetailsIndicatorChartComponent,
			new StatisticsTableComponent,
			new ErrorTableComponent(dataReader.errors(None, None)),
			activeSessionsChartComponent,
			responseTimeDistributionChartComponent,
			requestsChartComponent,
			responsesChartComponent)

		new TemplateWriter(globalFile(runOn)).writeToFile(template.getOutput)
	}

	private def count(records: Seq[IntVsTimePlot]): Int = records.iterator.map(_.value).sum
}

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

import com.excilys.ebi.gatling.charts.component.{ Component, ComponentLibrary, StatisticsTableComponent }
import com.excilys.ebi.gatling.charts.config.ChartsFiles.globalFile
import com.excilys.ebi.gatling.charts.template.GlobalPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors._
import com.excilys.ebi.gatling.core.result.{ IntVsTimePlot, PieSlice, Series }
import com.excilys.ebi.gatling.core.result.message.{ KO, OK }
import com.excilys.ebi.gatling.core.result.reader.DataReader

class GlobalReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		def activeSessionsChartComponent = {
			val activeSessionsSeries: Seq[Series[IntVsTimePlot]] = dataReader
				.scenarioNames
				.map { scenarioName => scenarioName -> dataReader.numberOfActiveSessionsPerSecond(Some(scenarioName)) }
				.reverse
				.zip(Vector(BLUE, GREEN, RED, YELLOW, CYAN, LIME, PURPLE, PINK, LIGHT_BLUE, LIGHT_ORANGE, LIGHT_RED, LIGHT_LIME, LIGHT_PURPLE, LIGHT_PINK))
				.map { case ((scenarioName, data), color) => new Series[IntVsTimePlot](scenarioName, data, Vector(color)) }

			componentLibrary.getActiveSessionsChartComponent(dataReader.runStart, activeSessionsSeries)
		}

		def requestsChartComponent: Component = {
			val all = dataReader.numberOfRequestsPerSecond().sortBy(_.time)
			val oks = dataReader.numberOfRequestsPerSecond(Some(OK)).sortBy(_.time)
			val kos = dataReader.numberOfRequestsPerSecond(Some(KO)).sortBy(_.time)

			val allSeries = new Series[IntVsTimePlot]("All requests", all, Vector(BLUE))
			val kosSeries = new Series[IntVsTimePlot]("Failed requests", kos, Vector(RED))
			val oksSeries = new Series[IntVsTimePlot]("Succeeded requests", oks, Vector(GREEN))
			val pieRequestsSeries = new Series[PieSlice]("Distribution", PieSlice("Success", count(oks)) :: PieSlice("Failures", count(kos)) :: Nil, Vector(GREEN, RED))

			componentLibrary.getRequestsChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
		}

		def transactionsChartComponent: Component = {
			val all = dataReader.numberOfTransactionsPerSecond().sortBy(_.time)
			val oks = dataReader.numberOfTransactionsPerSecond(Some(OK)).sortBy(_.time)
			val kos = dataReader.numberOfTransactionsPerSecond(Some(KO)).sortBy(_.time)

			val allSeries = new Series[IntVsTimePlot]("All transactions", all, Vector(BLUE))
			val kosSeries = new Series[IntVsTimePlot]("Failed transactions", kos, Vector(RED))
			val oksSeries = new Series[IntVsTimePlot]("Succeeded transactions", oks, Vector(GREEN))
			val pieRequestsSeries = new Series[PieSlice]("Distribution", PieSlice("Success", count(oks)) :: PieSlice("Failures", count(kos)) :: Nil, Vector(GREEN, RED))

			componentLibrary.getTransactionsChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
		}

		def responseTimeDistributionChartComponent: Component = {
			val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100)
			val okDistributionSeries = new Series[IntVsTimePlot]("Success", okDistribution, Vector(BLUE))
			val koDistributionSeries = new Series[IntVsTimePlot]("Failure", koDistribution, Vector(RED))

			componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
		}

		def statisticsComponent: Component = componentLibrary.getNumberOfRequestsChartComponent

		def statisticsTableComponent: Component = new StatisticsTableComponent

		def indicatorChartComponent: Component = componentLibrary.getRequestDetailsIndicatorChartComponent

		val template = new GlobalPageTemplate(
			statisticsComponent,
			indicatorChartComponent,
			statisticsTableComponent,
			activeSessionsChartComponent,
			responseTimeDistributionChartComponent,
			requestsChartComponent,
			transactionsChartComponent)

		new TemplateWriter(globalFile(runOn)).writeToFile(template.getOutput)
	}

	private def count(records: Seq[IntVsTimePlot]): Int = records.iterator.map(_.value).sum
}

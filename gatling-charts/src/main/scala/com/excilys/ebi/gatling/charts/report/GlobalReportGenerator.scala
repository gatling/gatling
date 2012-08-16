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
import com.excilys.ebi.gatling.charts.config.ChartsFiles.globalFile
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.GlobalPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ toString, YELLOW, RED, PURPLE, PINK, LIME, LIGHT_RED, LIGHT_PURPLE, LIGHT_PINK, LIGHT_ORANGE, LIGHT_LIME, LIGHT_BLUE, GREEN, CYAN, BLUE }
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ OK, KO }
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration

class GlobalReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		def activeSessionsChartComponent = {
			val activeSessionsSeries = dataReader
				.scenarioNames
				.map { scenarioName => scenarioName -> dataReader.numberOfActiveSessionsPerSecond(Some(scenarioName)) }
				.reverse
				.zip(List(BLUE, GREEN, RED, YELLOW, CYAN, LIME, PURPLE, PINK, LIGHT_BLUE, LIGHT_ORANGE, LIGHT_RED, LIGHT_LIME, LIGHT_PURPLE, LIGHT_PINK))
				.map { case ((scenarioName, data), color) => new Series[Long, Long](scenarioName, data, List(color)) }

			componentLibrary.getActiveSessionsChartComponent(activeSessionsSeries)
		}

		def requestsChartComponent = {
			val all = dataReader.numberOfRequestsPerSecond().sortBy(_._1)
			val oks = dataReader.numberOfRequestsPerSecond(Some(OK)).sortBy(_._1)
			val kos = dataReader.numberOfRequestsPerSecond(Some(KO)).sortBy(_._1)

			val allSeries = new Series[Long, Long]("All requests", all, List(BLUE))
			val kosSeries = new Series[Long, Long]("Failed requests", kos, List(RED))
			val oksSeries = new Series[Long, Long]("Succeeded requests", oks, List(GREEN))
			val pieRequestsSeries = new Series[String, Long]("Distribution", ("Success", count(oks)) :: ("Failures", count(kos)) :: Nil, List(GREEN, RED))

			componentLibrary.getRequestsChartComponent(allSeries, kosSeries, oksSeries, pieRequestsSeries)
		}

		def transactionsChartComponent = {
			val all = dataReader.numberOfTransactionsPerSecond().sortBy(_._1)
			val oks = dataReader.numberOfTransactionsPerSecond(Some(OK)).sortBy(_._1)
			val kos = dataReader.numberOfTransactionsPerSecond(Some(KO)).sortBy(_._1)

			val allSeries = new Series[Long, Long]("All requests", all, List(BLUE))
			val kosSeries = new Series[Long, Long]("Failed requests", kos, List(RED))
			val oksSeries = new Series[Long, Long]("Succeeded requests", oks, List(GREEN))
			val pieRequestsSeries = new Series[String, Long]("Distribution", ("Success", count(oks)) :: ("Failures", count(kos)) :: Nil, List(GREEN, RED))

			componentLibrary.getTransactionsChartComponent(allSeries, kosSeries, oksSeries, pieRequestsSeries)
		}

		def responseTimeDistributionChartComponent = {
			val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(configuration.chartingMaxPlotPerHistogram)
			val okDistributionSeries = new Series[Long, Long]("Success", okDistribution, List(BLUE))
			val koDistributionSeries = new Series[Long, Long]("Failure", koDistribution, List(RED))

			componentLibrary.getRequestDetailsResponseTimeDistributionChartComponent(okDistributionSeries, koDistributionSeries)
		}

		def statisticsComponent = new StatisticsTextComponent

		def indicatorChartComponent = componentLibrary.getRequestDetailsIndicatorChartComponent

		val template = new GlobalPageTemplate(
			statisticsComponent,
			indicatorChartComponent,
			activeSessionsChartComponent,
			responseTimeDistributionChartComponent,
			requestsChartComponent,
			transactionsChartComponent)

		new TemplateWriter(globalFile(runOn)).writeToFile(template.getOutput)
	}

	private def count(records: Seq[(Long, Long)]) = records.foldLeft(0L)((sum, entry) => sum + entry._2)

}

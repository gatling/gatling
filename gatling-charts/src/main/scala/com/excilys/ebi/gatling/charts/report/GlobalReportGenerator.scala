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
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.GlobalPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ BLUE, CYAN, GREEN, LIGHT_BLUE, LIGHT_LIME, LIGHT_ORANGE, LIGHT_PINK, LIGHT_PURPLE, LIGHT_RED, LIME, PINK, PURPLE, RED, YELLOW }
import com.excilys.ebi.gatling.core.result.message.{ KO, OK }
import com.excilys.ebi.gatling.core.result.reader.DataReader

class GlobalReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		def activeSessionsChartComponent = {
			val activeSessionsSeries: Seq[Series[Int, Int]] = dataReader
				.scenarioNames
				.map { scenarioName => scenarioName -> dataReader.numberOfActiveSessionsPerSecond(Some(scenarioName)) }
				.reverse
				.zip(List(BLUE, GREEN, RED, YELLOW, CYAN, LIME, PURPLE, PINK, LIGHT_BLUE, LIGHT_ORANGE, LIGHT_RED, LIGHT_LIME, LIGHT_PURPLE, LIGHT_PINK))
				.map { case ((scenarioName, data), color) => new Series[Int, Int](scenarioName, data, List(color)) }

			componentLibrary.getActiveSessionsChartComponent(dataReader.runStart, activeSessionsSeries)
		}

		def requestsChartComponent: Component = {
			val all = dataReader.numberOfRequestsPerSecond().sortBy(_._1)
			val oks = dataReader.numberOfRequestsPerSecond(Some(OK)).sortBy(_._1)
			val kos = dataReader.numberOfRequestsPerSecond(Some(KO)).sortBy(_._1)

			val allSeries = new Series[Int, Int]("All requests", all, List(BLUE))
			val kosSeries = new Series[Int, Int]("Failed requests", kos, List(RED))
			val oksSeries = new Series[Int, Int]("Succeeded requests", oks, List(GREEN))
			val pieRequestsSeries = new Series[String, Int]("Distribution", ("Success", count(oks)) :: ("Failures", count(kos)) :: Nil, List(GREEN, RED))

			componentLibrary.getRequestsChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
		}

		def transactionsChartComponent: Component = {
			val all = dataReader.numberOfTransactionsPerSecond().sortBy(_._1)
			val oks = dataReader.numberOfTransactionsPerSecond(Some(OK)).sortBy(_._1)
			val kos = dataReader.numberOfTransactionsPerSecond(Some(KO)).sortBy(_._1)

			val allSeries = new Series[Int, Int]("All transactions", all, List(BLUE))
			val kosSeries = new Series[Int, Int]("Failed transactions", kos, List(RED))
			val oksSeries = new Series[Int, Int]("Succeeded transactions", oks, List(GREEN))
			val pieRequestsSeries = new Series[String, Int]("Distribution", ("Success", count(oks)) :: ("Failures", count(kos)) :: Nil, List(GREEN, RED))

			componentLibrary.getTransactionsChartComponent(dataReader.runStart, allSeries, kosSeries, oksSeries, pieRequestsSeries)
		}

		def responseTimeDistributionChartComponent: Component = {
			val (okDistribution, koDistribution) = dataReader.responseTimeDistribution(100)
			val okDistributionSeries = new Series[Int, Int]("Success", okDistribution, List(BLUE))
			val koDistributionSeries = new Series[Int, Int]("Failure", koDistribution, List(RED))

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

	private def count(records: Seq[(Int, Int)]): Int = records.iterator.map(_._2).sum
}

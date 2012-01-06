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

import com.excilys.ebi.gatling.charts.component.ComponentLibrary
import com.excilys.ebi.gatling.charts.computer.Computer.{ numberOfSuccessfulRequestsPerSecond, numberOfRequestsPerSecondAsList, numberOfFailedRequestsPerSecond }
import com.excilys.ebi.gatling.charts.config.ChartsFiles.globalRequestsFile
import com.excilys.ebi.gatling.charts.loader.DataLoader
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.series.SharedSeries
import com.excilys.ebi.gatling.charts.template.RequestsPageTemplate
import com.excilys.ebi.gatling.charts.writer.TemplateWriter
import com.excilys.ebi.gatling.charts.util.Colors._

class RequestsReportGenerator(runOn: String, dataLoader: DataLoader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataLoader, componentLibrary) {

	def generate = {

		// Get Data
		val allRequestsData = numberOfRequestsPerSecondAsList(dataLoader.dataIndexedByDateWithoutMillis)
		val failedRequestsData = numberOfFailedRequestsPerSecond(dataLoader.dataIndexedByDateWithoutMillis)
		val succeededRequestsData = numberOfSuccessfulRequestsPerSecond(dataLoader.dataIndexedByDateWithoutMillis)
		val pieData = ("Success", succeededRequestsData.map(_._2).sum) :: ("Failures", failedRequestsData.map(_._2).sum) :: Nil

		// Create series
		val allRequests = new Series[DateTime, Int]("All requests", allRequestsData, List(BLUE))
		val failedRequests = new Series[DateTime, Int]("Failed requests", failedRequestsData, List(RED))
		val succeededRequests = new Series[DateTime, Int]("Succeeded requests", succeededRequestsData, List(GREEN))
		val pieSeries = new Series[String, Int]("Repartition", pieData, List(GREEN, RED))

		// Create template
		val template = new RequestsPageTemplate(componentLibrary.getRequestsChartComponent(allRequests, failedRequests, succeededRequests, pieSeries, SharedSeries.getAllActiveSessionsSeries))

		// Write template result to file
		new TemplateWriter(globalRequestsFile(runOn)).writeToFile(template.getOutput)
	}
}
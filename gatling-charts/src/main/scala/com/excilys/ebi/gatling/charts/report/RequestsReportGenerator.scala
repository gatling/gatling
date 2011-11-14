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
import com.excilys.ebi.gatling.charts.computer.Computer.numberOfFailedRequestsPerSecond
import com.excilys.ebi.gatling.charts.computer.Computer.numberOfRequestsPerSecondAsList
import com.excilys.ebi.gatling.charts.computer.Computer.numberOfSuccessfulRequestsPerSecond
import com.excilys.ebi.gatling.charts.loader.DataLoader
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.RequestsPageTemplate
import com.excilys.ebi.gatling.charts.util.PathHelper.GATLING_CHART_GLOBAL_REQUESTS_FILE
import com.excilys.ebi.gatling.charts.writer.TemplateWriter

class RequestsReportGenerator(runOn: String, dataLoader: DataLoader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataLoader, componentLibrary) {

	def generate = {
		// Get Data
		val allRequestsData = numberOfRequestsPerSecondAsList(dataLoader.dataIndexedByDateInSeconds)
		val failedRequestsData = numberOfFailedRequestsPerSecond(dataLoader.dataIndexedByDateInSeconds)
		val succeededRequestsData = numberOfSuccessfulRequestsPerSecond(dataLoader.dataIndexedByDateInSeconds)
		val pieData = ("Success", succeededRequestsData.map { entry => entry._2 }.sum) :: ("Failures", failedRequestsData.map { entry => entry._2 }.sum) :: Nil

		// Create series
		val allRequests = new Series[DateTime, Int]("All requests", allRequestsData)
		val failedRequests = new Series[DateTime, Int]("Failed requests", failedRequestsData)
		val succeededRequests = new Series[DateTime, Int]("Succeeded requests", succeededRequestsData)
		val pieSeries = new Series[String, Int]("Repartition", pieData)

		// Create template
		val template = new RequestsPageTemplate(componentLibrary.getRequestsChartComponent(allRequests, failedRequests, succeededRequests, pieSeries))

		// Write template result to file
		new TemplateWriter(runOn, GATLING_CHART_GLOBAL_REQUESTS_FILE).writeToFile(template.getOutput)
	}
}
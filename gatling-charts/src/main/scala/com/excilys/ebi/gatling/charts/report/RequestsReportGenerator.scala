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
import com.excilys.ebi.gatling.charts.component.ComponentLibrary
import com.excilys.ebi.gatling.charts.config.ChartsFiles.globalRequestsFile
import com.excilys.ebi.gatling.charts.series.Series
import com.excilys.ebi.gatling.charts.template.RequestsPageTemplate
import com.excilys.ebi.gatling.charts.util.Colors.{ toString, RED, GREEN, BLUE }
import com.excilys.ebi.gatling.charts.util.StatisticsHelper.{ count, numberOfRequestsPerSecondAsList, numberOfRequestsPerSecond }
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ OK, KO }
import com.excilys.ebi.gatling.core.result.reader.DataReader

class RequestsReportGenerator(runOn: String, dataReader: DataReader, componentLibrary: ComponentLibrary) extends ReportGenerator(runOn, dataReader, componentLibrary) {

	def generate {
		
		// Get Data
		val requestData = dataReader.realRequestRecordsGroupByExecutionStartDateInSeconds

		val allRequestsData = numberOfRequestsPerSecondAsList(requestData)
		val succeededRequestsData = numberOfRequestsPerSecond(requestData, OK)
		val failedRequestsData = numberOfRequestsPerSecond(requestData, KO)
		val pieData = ("Success", count(succeededRequestsData)) :: ("Failures", count(failedRequestsData)) :: Nil
				
		// Create series
		val allRequests = new Series[Long, Int]("All requests", allRequestsData, List(BLUE))
		val failedRequests = new Series[Long, Int]("Failed requests", failedRequestsData, List(RED))
		val succeededRequests = new Series[Long, Int]("Succeeded requests", succeededRequestsData, List(GREEN))
		val pieSeries = new Series[String, Int]("Repartition", pieData, List(GREEN, RED))

		// Create template
		val template = new RequestsPageTemplate(componentLibrary.getRequestsChartComponent(allRequests, failedRequests, succeededRequests, pieSeries))

		// Write template result to file
		new TemplateWriter(globalRequestsFile(runOn)).writeToFile(template.getOutput)
	}
}
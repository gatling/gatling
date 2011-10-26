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
package com.excilys.ebi.gatling.statistics.presenter

import com.excilys.ebi.gatling.core.util.PathHelper._

import com.excilys.ebi.gatling.statistics.extractor.GlobalRequestsDataExtractor
import com.excilys.ebi.gatling.statistics.template.GlobalRequestsTemplate
import com.excilys.ebi.gatling.statistics.template.TimeSeries
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter
import com.excilys.ebi.gatling.statistics.writer.TSVFileWriter
import com.excilys.ebi.gatling.statistics.utils.HighChartsHelper._

import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ListBuffer

class GlobalRequestsDataPresenter extends DataPresenter[List[(String, (Double, Double, Double))]] {

	def generateGraphFor(runOn: String, results: List[(String, (Double, Double, Double))]) {
		var globalData: List[(String, Double)] = Nil
		var successData: List[(String, Double)] = Nil
		var failureData: List[(String, Double)] = Nil
		var forFile: List[List[String]] = Nil

		results.foreach {
			case (date, (numberOfRequests, numberOfSuccesses, numberOfFailures)) =>
				val formattedDate = printHighChartsDate(date)

				globalData = (formattedDate, numberOfRequests) :: globalData
				successData = (formattedDate, numberOfSuccesses) :: successData
				failureData = (formattedDate, numberOfFailures) :: failureData

				forFile = List(date, numberOfRequests.toString, numberOfSuccesses.toString, numberOfFailures.toString) :: forFile
		}

		new TSVFileWriter(runOn, GATLING_STATS_GLOBAL_REQUESTS_FILE).writeToFile(forFile)

		val series = List(new TimeSeries("All", globalData), new TimeSeries("Success", successData), new TimeSeries("Failures", failureData))

		val output = new GlobalRequestsTemplate(runOn, series).getOutput

		new TemplateWriter(runOn, GATLING_GRAPH_GLOBAL_REQUESTS_FILE).writeToFile(output)
	}
}

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

import com.excilys.ebi.gatling.statistics.series.SharedSeries._
import com.excilys.ebi.gatling.statistics.series.TimeSeries
import com.excilys.ebi.gatling.statistics.template.ActiveSessionsTemplate
import com.excilys.ebi.gatling.statistics.utils.HighChartsHelper._
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter
import com.excilys.ebi.gatling.statistics.writer.TSVFileWriter

import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ListBuffer

class ActiveSessionsDataPresenter extends DataPresenter[LinkedHashMap[String, ListBuffer[(String, Double)]]] {

	def generateChartFor(runOn: String, results: LinkedHashMap[String, ListBuffer[(String, Double)]]) = {

		// TODO: write file with results
		//new TSVFileWriter(runOn, "active_sessions.tsv").writeToFile(results.map { e => List(e._1, e._2.toString) })

		var seriesList: List[TimeSeries] = Nil

		results.map {
			result =>
				val (scenarioName, mutableListOfValues) = result
				val listOfValues = mutableListOfValues.toList
				val series = new TimeSeries(scenarioName, listOfValues.map { e => (printHighChartsDate(e._1), e._2) }, 0)
				seriesList = series :: seriesList

				if (scenarioName == ALL_ACTIVE_SESSIONS)
					share(ALL_ACTIVE_SESSIONS, series)
		}

		val output = new ActiveSessionsTemplate(runOn, seriesList).getOutput

		new TemplateWriter(runOn, GATLING_CHART_ACTIVE_SESSIONS_FILE).writeToFile(output)
	}
}
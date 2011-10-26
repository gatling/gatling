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

import com.excilys.ebi.gatling.core.util.FileHelper._
import com.excilys.ebi.gatling.statistics.extractor.DetailsRequestsDataExtractor
import com.excilys.ebi.gatling.statistics.result.DetailsRequestsDataResult
import com.excilys.ebi.gatling.statistics.template.DetailsRequestsTemplate
import com.excilys.ebi.gatling.statistics.template.TimeSeries
import com.excilys.ebi.gatling.statistics.template.ColumnSeries
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter
import com.excilys.ebi.gatling.statistics.writer.TSVFileWriter
import com.excilys.ebi.gatling.statistics.utils.HighChartsHelper._
import scala.collection.immutable.TreeMap

class DetailsRequestsDataPresenter extends DataPresenter[Map[String, DetailsRequestsDataResult]] {

	def generateGraphFor(runOn: String, results: Map[String, DetailsRequestsDataResult]) = {
		results.foreach {
			case (requestName, result) =>

				new TSVFileWriter(runOn, formatToFilename(requestName) + TSV_EXTENSION).writeToFile(result.timeValues.map { e => List(e._1, e._2.toString) })

				val series = List(new TimeSeries(requestName.substring(8), result.timeValues.map { e => (printHighChartsDate(e._1), e._2) }),
					new TimeSeries("medium", result.timeValues.map { e => (printHighChartsDate(e._1), result.medium) }))

				val columnData = new ColumnSeries(requestName.substring(8), result.columnData._1, result.columnData._2)

				val output = new DetailsRequestsTemplate(runOn, series, columnData, requestName, result).getOutput

				new TemplateWriter(runOn, formatToFilename(requestName) + HTML_EXTENSION).writeToFile(output)
		}
	}
}
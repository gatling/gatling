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
import com.excilys.ebi.gatling.statistics.series.ColumnSeries
import com.excilys.ebi.gatling.statistics.series.SharedSeries._
import com.excilys.ebi.gatling.statistics.series.TimeSeries
import com.excilys.ebi.gatling.statistics.series.PieSeries
import com.excilys.ebi.gatling.statistics.template.DetailsRequestsTemplate
import com.excilys.ebi.gatling.statistics.utils.HighChartsHelper._
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter
import com.excilys.ebi.gatling.statistics.writer.TSVFileWriter
import scala.collection.immutable.TreeMap

class DetailsRequestsDataPresenter extends DataPresenter[Map[String, DetailsRequestsDataResult]] {

	def generateChartFor(runOn: String, results: Map[String, DetailsRequestsDataResult]) = {
		results.foreach {
			case (requestName, result) =>

				new TSVFileWriter(runOn, formatToFilename(requestName) + TSV_EXTENSION).writeToFile(result.timeValues.map { e => List(e._1, e._2.toString) })

				val series = List(getShared(ALL_ACTIVE_SESSIONS).asInstanceOf[TimeSeries], new TimeSeries(requestName.substring(8), result.timeValues.map { e => (printHighChartsDate(e._1), e._2) }, 1))

				val dispersionData = new ColumnSeries(requestName.substring(8), result.columnData._1.map { _.toString }, result.columnData._2)

				val indicatorData = getIndicatorData(result)

				val indicatorsData = new ColumnSeries("Indicators", indicatorData._1, indicatorData._2)
				
				val pieData = for{
					key <- List("ok", "medium", "ko");
					numberOfRequests <- indicatorData._2;
					color <- List("#89A54E", "#FF9100", "#AA4643")
				} yield (key, numberOfRequests.toInt, color)
					
				
				
				val indicatorsPieData = new PieSeries("Result", pieData)

				val output = new DetailsRequestsTemplate(runOn, series, dispersionData, indicatorsData, indicatorsPieData, requestName, result).getOutput

				new TemplateWriter(runOn, formatToFilename(requestName) + HTML_EXTENSION).writeToFile(output)
		}
	}

	def getIndicatorData(result: DetailsRequestsDataResult): (List[String], List[Double]) = {

		val (okGroup, mediumGroup, koGroup) = ("ok", "medium", "ko")

		val data = result.columnData._1 zip result.columnData._2

		val map = data.groupBy {
			case (time, numberOfRequests) if (time < 100) => okGroup
			case (time, numberOfRequests) if (time > 500) => koGroup
			case _ => mediumGroup
		}.mapValues { list =>
			list.unzip._2.sum
		}

		logger.warn("map: {}", map)

		(List("< 100ms", "100ms < t < 500ms", "> 500ms"), List(map.get("ok").getOrElse(0), map.get("medium").getOrElse(0), map.get("ko").getOrElse(0)))
	}
}
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

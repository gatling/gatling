package com.excilys.ebi.gatling.statistics.presenter

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.statistics.extractor.GlobalRequestsDataExtractor
import com.excilys.ebi.gatling.statistics.template.GlobalRequestsTemplate
import com.excilys.ebi.gatling.statistics.template.Series
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter
import com.excilys.ebi.gatling.statistics.writer.TSVFileWriter

class GlobalRequestsDataPresenter extends DataPresenter with Logging {
  def generateGraphFor(runOn: String, menuItems: Map[String, String]) = {
    var globalData: List[(String, Double)] = Nil
    var successData: List[(String, Double)] = Nil
    var failureData: List[(String, Double)] = Nil
    var forFile: List[List[String]] = Nil

    new GlobalRequestsDataExtractor(runOn).getResults foreach {
      case (date, (numberOfRequests, numberOfSuccesses, numberOfFailures)) =>
        val formattedDate = getDateForHighcharts(date)

        globalData = (formattedDate, numberOfRequests) :: globalData
        successData = (formattedDate, numberOfSuccesses) :: successData
        failureData = (formattedDate, numberOfFailures) :: failureData

        forFile = List(date, numberOfRequests.toString, numberOfSuccesses.toString, numberOfFailures.toString) :: forFile
    }

    new TSVFileWriter(runOn, "requests.tsv").writeToFile(forFile)

    val series = List(new Series("All", globalData), new Series("Success", successData), new Series("Failures", failureData))

    val output = new GlobalRequestsTemplate(runOn, menuItems, series).getOutput

    new TemplateWriter(runOn, "requests.html").writeToFile(output)
  }
}

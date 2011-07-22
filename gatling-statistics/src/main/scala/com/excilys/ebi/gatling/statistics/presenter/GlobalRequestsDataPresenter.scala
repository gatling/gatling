package com.excilys.ebi.gatling.statistics.presenter

import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.statistics.extractor.GlobalRequestsDataExtractor
import com.excilys.ebi.gatling.statistics.template.GlobalRequestsTemplate
import com.excilys.ebi.gatling.statistics.template.Series
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter

class GlobalRequestsDataPresenter extends DataPresenter with Logging {
  def generateGraphFor(runOn: String, menuItems: Map[String, String]) = {
    var globalData: List[(String, Int)] = Nil
    var successData: List[(String, Int)] = Nil
    var failureData: List[(String, Int)] = Nil

    new GlobalRequestsDataExtractor(runOn).getResults foreach {
      case (date, (numberOfRequests, numberOfSuccesses, numberOfFailures)) =>
        val formattedDate = getDateForHighcharts(date)

        globalData = (formattedDate, numberOfRequests) :: globalData
        successData = (formattedDate, numberOfSuccesses) :: successData
        failureData = (formattedDate, numberOfFailures) :: failureData
    }

    val series = List(new Series("All", globalData), new Series("Success", successData), new Series("Failures", failureData))

    val output = new GlobalRequestsTemplate(runOn, menuItems, series).getOutput

    new TemplateWriter(runOn, "requests.html").writeToFile(output)
  }
}

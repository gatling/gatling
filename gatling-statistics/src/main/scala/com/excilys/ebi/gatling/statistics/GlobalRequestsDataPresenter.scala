package com.excilys.ebi.gatling.statistics;

import com.excilys.ebi.gatling.core.log.Logging
import org.fusesource.scalate._

import java.io.FileWriter
import java.io.File

class GlobalRequestsDataPresenter extends Logging {
  def generateGraphFor(runOn: String, menuItems: Map[String, String]) = {
    var dates: List[String] = Nil
    var globalValues: List[Int] = Nil
    var okValues: List[Int] = Nil
    var koValues: List[Int] = Nil

    new GlobalRequestsDataExtractor(runOn).getResults foreach {
      case (date, (numberOfRequests, numberOfSuccesses, numberOfFailures)) =>
        dates = date.substring(11) :: dates
        globalValues = numberOfRequests :: globalValues
        okValues = numberOfSuccesses :: okValues
        koValues = numberOfFailures :: koValues
    }

    logger.debug("Dates: {}\nValues: {}", dates, (globalValues, okValues, koValues))

    val output = new GlobalRequestsTemplate(runOn, menuItems, dates.reverse, globalValues.reverse, okValues.reverse, koValues.reverse).getOutput

    new TemplateWriter(runOn, "requests.html").writeToFile(output)
  }
}

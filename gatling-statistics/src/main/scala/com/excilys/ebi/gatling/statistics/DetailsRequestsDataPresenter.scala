package com.excilys.ebi.gatling.statistics

import com.excilys.ebi.gatling.core.log.Logging
import org.fusesource.scalate._

import scala.collection.immutable.TreeMap

import java.io.File
import java.io.FileWriter

class DetailsRequestsDataPresenter extends Logging {
  def generateGraphFor(runOn: String): Map[String, String] = {
    var menuItems: Map[String, String] = TreeMap.empty

    val results = new DetailsRequestsDataExtractor(runOn).getResults

    results.foreach {
      case (requestName, result) =>
        val fileName = requestNameToFileName(requestName) + ".html"
        menuItems = menuItems + (requestName.substring(8) -> fileName)
    }

    results.foreach {
      case (requestName, result) =>

        var dates: List[String] = Nil
        var values: List[Int] = Nil

        result.values foreach {
          case (date, responseTime) =>
            dates = date.substring(11) :: dates
            values = responseTime :: values
        }

        logger.debug("Dates: {}\nValues: {}", dates, values)

        val output = new DetailsRequestsTemplate(runOn, menuItems, dates.reverse, values.reverse, requestName, result).getOutput

        new TemplateWriter(runOn, requestNameToFileName(requestName) + ".html").writeToFile(output)

    }
    menuItems
  }

  private def requestNameToFileName(requestName: String): String = requestName.replace("-", "_").replace(" ", "_").toLowerCase
}
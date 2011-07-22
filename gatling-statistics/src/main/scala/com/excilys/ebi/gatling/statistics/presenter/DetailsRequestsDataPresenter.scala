package com.excilys.ebi.gatling.statistics.presenter

import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.statistics.extractor.DetailsRequestsDataExtractor
import com.excilys.ebi.gatling.statistics.template.DetailsRequestsTemplate
import com.excilys.ebi.gatling.statistics.template.Series
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter

import scala.collection.immutable.TreeMap

class DetailsRequestsDataPresenter extends DataPresenter with Logging {

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

        val series = List(new Series(requestName.substring(8), result.values.map { e => (getDateForHighcharts(e._1), e._2) }))

        val output = new DetailsRequestsTemplate(runOn, menuItems, series, requestName, result).getOutput

        new TemplateWriter(runOn, requestNameToFileName(requestName) + ".html").writeToFile(output)

    }
    menuItems
  }

  private def requestNameToFileName(requestName: String): String = requestName.replace("-", "_").replace(" ", "_").replace("'", "").toLowerCase
}
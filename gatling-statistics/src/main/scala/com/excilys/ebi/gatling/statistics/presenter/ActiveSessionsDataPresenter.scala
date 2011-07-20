package com.excilys.ebi.gatling.statistics.presenter

import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.statistics.extractor.ActiveSessionsDataExtractor
import com.excilys.ebi.gatling.statistics.template.ActiveSessionsTemplate
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter

class ActiveSessionsDataPresenter extends Logging {
  def generateGraphFor(runOn: String, menuItems: Map[String, String]) = {
    val title = "Active Sessions"

    var dates: List[String] = Nil
    var values: List[Int] = Nil

    new ActiveSessionsDataExtractor(runOn).getResults foreach {
      case (date, numberOfActiveSessions) =>
        dates = date.substring(11) :: dates
        values = numberOfActiveSessions :: values
    }

    logger.debug("Dates: {}\nValues: {}", dates, values)

    val output = new ActiveSessionsTemplate(runOn, menuItems, dates.reverse, values.reverse).getOutput

    new TemplateWriter(runOn, "active_sessions.html").writeToFile(output)
  }
}

package com.excilys.ebi.gatling.statistics.presenter

import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.statistics.extractor.ActiveSessionsDataExtractor
import com.excilys.ebi.gatling.statistics.template.ActiveSessionsTemplate
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter

class ActiveSessionsDataPresenter extends DataPresenter with Logging {
  def generateGraphFor(runOn: String, menuItems: Map[String, String]) = {
    new TemplateWriter(runOn, "active_sessions.html")
      .writeToFile(
        new ActiveSessionsTemplate(runOn, menuItems, new ActiveSessionsDataExtractor(runOn).getResults.map { e => (getDateForHighcharts(e._1), e._2) })
          .getOutput)
  }
}

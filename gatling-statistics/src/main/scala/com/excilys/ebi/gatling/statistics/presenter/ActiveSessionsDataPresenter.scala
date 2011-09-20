package com.excilys.ebi.gatling.statistics.presenter

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.PathHelper._

import com.excilys.ebi.gatling.statistics.extractor.ActiveSessionsDataExtractor
import com.excilys.ebi.gatling.statistics.template.ActiveSessionsTemplate
import com.excilys.ebi.gatling.statistics.template.TimeSeries
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter
import com.excilys.ebi.gatling.statistics.writer.TSVFileWriter

class ActiveSessionsDataPresenter extends DataPresenter with Logging {
  def generateGraphFor(runOn: String, menuItems: Map[String, String]) = {

    val results = new ActiveSessionsDataExtractor(runOn).getResults

    // TODO: write file with results
    //new TSVFileWriter(runOn, "active_sessions.tsv").writeToFile(results.map { e => List(e._1, e._2.toString) })

    var seriesList: List[TimeSeries] = Nil

    results.map {
      result =>
        val (scenarioName, mutableListOfValues) = result
        val listOfValues = mutableListOfValues.toList
        seriesList = new TimeSeries(scenarioName, listOfValues.map { e => (getDateForHighcharts(e._1), e._2) }) :: seriesList
    }

    val output = new ActiveSessionsTemplate(runOn, menuItems, seriesList).getOutput

    new TemplateWriter(runOn, GATLING_GRAPH_ACTIVE_SESSIONS_FILE).writeToFile(output)
  }
}
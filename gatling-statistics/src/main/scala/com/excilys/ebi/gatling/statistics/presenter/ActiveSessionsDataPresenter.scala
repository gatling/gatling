package com.excilys.ebi.gatling.statistics.presenter

import com.excilys.ebi.gatling.core.util.PathHelper._
import com.excilys.ebi.gatling.statistics.utils.HighChartsHelper._

import com.excilys.ebi.gatling.statistics.template.ActiveSessionsTemplate
import com.excilys.ebi.gatling.statistics.template.TimeSeries
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter
import com.excilys.ebi.gatling.statistics.writer.TSVFileWriter

import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ListBuffer

class ActiveSessionsDataPresenter extends DataPresenter[LinkedHashMap[String, ListBuffer[(String, Double)]]] {

  def generateGraphFor(runOn: String, results: LinkedHashMap[String, ListBuffer[(String, Double)]], menuItems: Map[String, String]) = {

    // TODO: write file with results
    //new TSVFileWriter(runOn, "active_sessions.tsv").writeToFile(results.map { e => List(e._1, e._2.toString) })

    var seriesList: List[TimeSeries] = Nil

    results.map {
      result =>
        val (scenarioName, mutableListOfValues) = result
        val listOfValues = mutableListOfValues.toList
        seriesList = new TimeSeries(scenarioName, listOfValues.map { e => (printHighChartsDate(e._1), e._2) }) :: seriesList
    }

    val output = new ActiveSessionsTemplate(runOn, menuItems, seriesList).getOutput

    new TemplateWriter(runOn, GATLING_GRAPH_ACTIVE_SESSIONS_FILE).writeToFile(output)
  }
}
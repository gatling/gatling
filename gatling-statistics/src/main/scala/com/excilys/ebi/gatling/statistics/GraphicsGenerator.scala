package com.excilys.ebi.gatling.statistics

import java.io.File

import scala.io.Source

import org.apache.commons.io.FileUtils

import com.excilys.ebi.gatling.core.config.GatlingConfig._
import com.excilys.ebi.gatling.core.util.PathHelper._

import com.excilys.ebi.gatling.statistics.presenter.DetailsRequestsDataPresenter

class GraphicsGenerator {
  def generateFor(runOn: String) = {

    val jQueryFile = new File(GATLING_ASSETS_JQUERY)
    val highchartsFile = new File(GATLING_ASSETS_HIGHCHARTS)

    val jQueryFileDest = new File(GATLING_RESULTS_FOLDER + "/" + runOn + GATLING_JS_JQUERY)
    val highchartsFileDest = new File(GATLING_RESULTS_FOLDER + "/" + runOn + GATLING_JS_HIGHCHARTS)

    FileUtils.copyFile(jQueryFile, jQueryFileDest)
    FileUtils.copyFile(highchartsFile, highchartsFileDest)

    val menuItems = (new DetailsRequestsDataPresenter).generateGraphFor(runOn)

    val generator = new CompositeGraphicGenerator(new ActiveSessionsGraphicGenerator, new GlobalRequestsGraphicGenerator)

    for (line <- Source.fromFile(GATLING_RESULTS_FOLDER + "/" + runOn + "/" + GATLING_SIMULATION_LOG_FILE, CONFIG_GATLING_ENCODING).getLines) {
      line.split("\t") match {
        // If we have a well formated result
        case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage) => {

          generator.onRow(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage)
        }
        // Else, if the resulting data is not well formated print an error message
        case _ => sys.error("Input file not well formatted")
      }
    }

    generator.generateGraphFor(runOn, menuItems)
  }
}
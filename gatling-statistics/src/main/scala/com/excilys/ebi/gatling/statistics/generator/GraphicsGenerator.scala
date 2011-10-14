package com.excilys.ebi.gatling.statistics.generator

import java.io.File
import scala.io.Source
import org.apache.commons.io.FileUtils
import com.excilys.ebi.gatling.core.config.GatlingConfig._
import com.excilys.ebi.gatling.core.util.PathHelper._
import org.apache.commons.lang3.StringUtils

class GraphicsGenerator {
  def generateFor(runOn: String) = {

    val jQueryFile = new File(GATLING_ASSETS_JQUERY)
    val highchartsFile = new File(GATLING_ASSETS_HIGHCHARTS)

    val jQueryFileDest = new File(GATLING_RESULTS_FOLDER + "/" + runOn + GATLING_JS_JQUERY)
    val highchartsFileDest = new File(GATLING_RESULTS_FOLDER + "/" + runOn + GATLING_JS_HIGHCHARTS)

    FileUtils.copyFile(jQueryFile, jQueryFileDest)
    FileUtils.copyFile(highchartsFile, highchartsFileDest)

    val generator = new CompositeGraphicGenerator(new ActiveSessionsGraphicGenerator, new GlobalRequestsGraphicGenerator, new DetailsRequestsGraphicGenerator, new MenuItemsGraphicGenerator)

    for (line <- Source.fromFile(GATLING_RESULTS_FOLDER + "/" + runOn + "/" + GATLING_SIMULATION_LOG_FILE, CONFIG_GATLING_ENCODING).getLines) {
      line.split("\t") match {
        // If we have a well formated result
        case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage, groups) =>
          val groupsArray = StringUtils.stripAll(Array(groups), "|")(0).split("\\|")
          generator.onRow(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage, groupsArray.toList)
        // Else, if the resulting data is not well formated print an error message
        case _ => logger.warn("simulation.log had bad end of file, statistics will be generated but may not be accurate")
      }
    }

    generator.generateGraphFor(runOn)
  }
}

package com.excilys.ebi.gatling.statistics.generator

import scala.io.Source
import com.excilys.ebi.gatling.core.config.GatlingConfig._
import com.excilys.ebi.gatling.core.util.PathHelper._
import com.excilys.ebi.gatling.core.result.writer.FileDataWriter._
import scala.tools.nsc.io.File

class GraphicsGenerator {

	def generateFor(runOn: String) = {

		val jQueryFile = File(GATLING_ASSETS_JQUERY)
		val highchartsFile = File(GATLING_ASSETS_HIGHCHARTS)

		jQueryFile.copyTo(GATLING_RESULTS_FOLDER + "/" + runOn + GATLING_JS_JQUERY, true)
		highchartsFile.copyTo(GATLING_RESULTS_FOLDER + "/" + runOn + GATLING_JS_HIGHCHARTS, true)

		val generator = new CompositeGraphicGenerator(new ActiveSessionsGraphicGenerator, new GlobalRequestsGraphicGenerator, new DetailsRequestsGraphicGenerator, new MenuItemsGraphicGenerator)

		for (line <- Source.fromFile(GATLING_RESULTS_FOLDER + "/" + runOn + "/" + GATLING_SIMULATION_LOG_FILE, CONFIG_GATLING_ENCODING).getLines) {
			line.split("\t") match {
				// If we have a well formated result
				case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage, groups) =>
					val groupsArray = groups.stripPrefix(GROUPS_PREFIX).stripSuffix(GROUPS_SUFFIX).split(GROUPS_SEPARATOR)
					generator.onRow(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage, groupsArray.toList)
				// Else, if the resulting data is not well formated print an error message
				case _ => logger.warn("simulation.log had bad end of file, statistics will be generated but may not be accurate")
			}
		}

		generator.generateGraphFor(runOn)
	}
}
/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.app

import java.lang.System.currentTimeMillis
import scala.tools.nsc.io.Directory
import com.excilys.ebi.gatling.app.OptionsConstants._
import com.excilys.ebi.gatling.charts.report.ReportsGenerator
import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.{ GatlingConfiguration, GatlingFiles, GatlingOptions }
import com.excilys.ebi.gatling.core.scenario.configuration.Simulation
import grizzled.slf4j.Logging
import scopt.OptionParser
import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.runner.Selection

/**
 * Object containing entry point of application
 */
object Gatling extends Logging {

	/**
	 * Entry point of Application
	 *
	 * @param args Arguments of the main method
	 */
	def main(args: Array[String]) {

		val gatlingOptions = new GatlingOptions

		val cliOptsParser = new OptionParser("gatling") {
			opt(NO_REPORTS_OPTION, NO_REPORTS_ALIAS, "Runs simulation but does not generate reports", { gatlingOptions.noReports = true })
			opt(REPORTS_ONLY_OPTION, REPORTS_ONLY_ALIAS, "<directoryName>", "Generates the reports for the simulation in <directoryName>", { v: String => gatlingOptions.reportsOnlyDirectoryName = Some(v) })
			opt(CONFIG_FILE_OPTION, CONFIG_FILE_ALIAS, "<file>", "Uses <file> as the configuration file", { v: String => gatlingOptions.configFilePath = Some(v) })
			opt(DATA_FOLDER_OPTION, DATA_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> as the absolute path of the directory where feeders are stored", { v: String => gatlingOptions.dataDirectory = Some(v) })
			opt(RESULTS_FOLDER_OPTION, RESULTS_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> as the absolute path of the directory where results are stored", { v: String => gatlingOptions.resultsDirectory = Some(v) })
			opt(REQUEST_BODIES_FOLDER_OPTION, REQUEST_BODIES_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> as the absolute path of the directory where request bodies are stored", { v: String => gatlingOptions.requestBodiesDirectory = Some(v) })
			opt(SIMULATIONS_FOLDER_OPTION, SIMULATIONS_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> to discover simulations that could be run", { v: String => gatlingOptions.simulationSourcesDirectory = Some(Directory(v)) })
			opt(SIMULATIONS_BINARIES_FOLDER_OPTION, SIMULATIONS_BINARIES_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> to discover already compiled simulations", { v: String => gatlingOptions.simulationBinariesDirectory = Some(Directory(v)) })
			opt(SIMULATIONS_OPTION, SIMULATIONS_ALIAS, "<classNamesList>", "Runs the <classNamesList> simulations sequentially", { v: String => gatlingOptions.simulationClassNames = Some(v.split(",").map(_.trim).toList) })
			opt(RUN_NAME_OPTION, RUN_NAME_ALIAS, "<runName>", "Use <runName> for the output directory", { v: String => gatlingOptions.runName = v })
		}

		// if arguments are incorrect, usage message is displayed
		if (cliOptsParser.parse(args)) new Gatling(gatlingOptions).start
	}
}

class Gatling(options: GatlingOptions) extends Logging {

	// Initializes configuration
	GatlingConfiguration.setUp(options)

	def start {
		val runUuids = options.reportsOnlyDirectoryName
			.map(List(_))
			.getOrElse {
				val simulations = options.simulationBinariesDirectory
					.map( // expect simulations to have been pre-compiled (ex: IDE)
						SimulationClassLoader.fromClasspathBinariesDirectory(_))
					.getOrElse(SimulationClassLoader.fromSourcesDirectory(GatlingFiles.simulationSourcesDirectory))
					.simulationClasses(options.simulationClassNames)

				val selection = options.simulationClassNames match {
					case Some(_) => new Selection(simulations, options.runName, options.runName)
					case None => interactiveSelect(simulations, options)
				}

				new Runner(selection).run
			}

		if (!options.noReports)
			runUuids.foreach(generateReports)
	}

	private def interactiveSelect(simulations: List[Class[Simulation]], options: GatlingOptions): Selection = {

		val simulation = selectSimulationClass(simulations)

		println("Select run id (default is '" + GatlingOptions.DEFAULT_RUN_ID + "'). Accepted characters are a-z, A-Z, 0-9, - and _")
		val runId = {
			val userInput = Console.readLine.trim

			if (!userInput.matches("[\\w-_]*"))
				throw new IllegalArgumentException(userInput + " contains illegal characters")

			if (!userInput.isEmpty) userInput else options.runName
		}

		println("Select run description (optional)")
		val runDescription = Console.readLine.trim

		new Selection(List(simulation), runId, runDescription)
	}

	private def selectSimulationClass(simulations: List[Class[Simulation]]): Class[Simulation] = {

		val selection = simulations.size match {
			case 0 =>
				// If there is no simulation file
				println("There is no simulation script. Please check that your scripts are in user-files/simulations")
				sys.exit
			case 1 =>
				// If there is only one simulation file
				info("There is only one simulation, executing it.")
				0
			case size =>
				println("Choose a simulation number:")
				for (i <- 0 until size) {
					println("     [" + i + "] " + simulations(i).getName)
				}
				Console.readInt
		}

		val validRange = 0 until simulations.size
		if (validRange contains selection)
			simulations(selection)
		else {
			println("Invalid selection, must be in " + validRange)
			selectSimulationClass(simulations)
		}
	}

	/**
	 * This method call the statistics module to generate the charts and statistics
	 *
	 * @param runUuid The directory from which the simulation.log will be parsed
	 * @return Nothing
	 */
	private def generateReports(runUuid: String) {
		println("Generating reports...")
		val start = currentTimeMillis
		try {
			val indexFile = ReportsGenerator.generateFor(runUuid)
			println("Reports generated in " + (currentTimeMillis - start) / 1000 + "s.")
			println("Please open the following file : " + indexFile)

		} catch {
			case e => error("Reports weren't generated", e)
		}
	}
}

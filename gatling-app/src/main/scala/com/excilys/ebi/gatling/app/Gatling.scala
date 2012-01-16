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

import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ Set => MSet }
import scala.collection.mutable.{ MultiMap, HashMap }
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.Directory

import org.joda.time.DateTime

import com.excilys.ebi.gatling.app.compiler.{ TextScenarioCompiler, ScalaScenarioCompiler, IdeScenarioCompiler }
import com.excilys.ebi.gatling.charts.report.ReportsGenerator
import com.excilys.ebi.gatling.core.config.GatlingFiles.GATLING_SIMULATIONS_FOLDER
import com.excilys.ebi.gatling.core.config.GatlingConfig
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.DateHelper.printFileNameDate
import com.excilys.ebi.gatling.core.util.FileHelper.{ TXT_EXTENSION, SCALA_EXTENSION }

import CommandLineOptions.options.{ resultsFolder, requestBodiesFolder, reportsOnlyFolder, reportsOnly, noReports, ideSimulationPackage, ideSimulationFolder, dataFolder, configFileName }
import scopt.OptionParser

/**
 * Object containing entry point of application
 */
object Gatling extends Logging {

	val cliOptsParser =
		new OptionParser("gatling") {
			opt("nr", "no-reports", "Runs simulation but does not generate reports", { CommandLineOptions.setNoReports })
			opt("ro", "reports-only", "<folderName>", "Generates the reports for the simulation in <folderName>", { v: String => CommandLineOptions.setReportsOnly(v) })
			opt("cf", "config-file", "<fileName>", "Uses <fileName> as the configuration file", { v: String => CommandLineOptions.setConfigFileName(v) })
			opt("df", "data-folder", "<folderName>", "Uses <folderName> as the folder where feeders are stored", { v: String => CommandLineOptions.setDataFolder(v) })
			opt("rf", "results-folder", "<folderName>", "Uses <folderName> as the folder where results are stored", { v: String => CommandLineOptions.setResultsFolder(v) })
			opt("bf", "request-bodies-folder", "<folderName>", "Uses <folderName> as the folder where request bodies are stored", { v: String => CommandLineOptions.setRequestBodiesFolder(v) })
			opt("isf", "ide-simulations-folder", "<folderName>", "IDE & Maven Archetype Only -- Uses <folderName> to discover simulations that could be run", { v: String => CommandLineOptions.setIdeSimulationFolder(v) })
			opt("isp", "ide-simulations-package", "<packageName>", "IDE & Maven Archetype Only -- Uses <packageName> to start the simulations", { v: String => CommandLineOptions.setIdeSimulationPackage(v) })
		}

	/**
	 * Entry point of Application
	 *
	 * @param args Arguments of the main method
	 */
	def main(args: Array[String]) {

		if (cliOptsParser.parse(args))
			runGatling

		// if arguments are bad, usage message is displayed
	}

	def apply(dataFolder: String, resultsFolder: String, requestBodiesFolder: String, ideSimulationFolder: String, ideSimulationPackage: String) =
		main(Array("-df", dataFolder, "-rf", resultsFolder, "-bf", requestBodiesFolder, "-isf", ideSimulationFolder, "-isp", ideSimulationPackage))

	private def runGatling = {
		println("-----------\nGatling cli\n-----------\n")

		import CommandLineOptions.options._
		GatlingConfig(configFileName, dataFolder, requestBodiesFolder, resultsFolder) // Initializes configuration

		val folderName =
			if (ideSimulationFolder.isDefined)
				displayMenuAndRunForIde
			else
				displayMenuAndRun

		// Generation of statistics
		if (!noReports) {
			println("Simulation Finished. Generating Reports...")
			generateStats(folderName)
			println("Reports Generated. All done.")
		}
	}

	private def displayMenuAndRun: String = {
		import CommandLineOptions.options._

		// Getting files in scenarios folder
		val files = Directory(GATLING_SIMULATIONS_FOLDER).files.map(_.name).filter(name => name.endsWith(TXT_EXTENSION) || name.endsWith(SCALA_EXTENSION)).filterNot(_.startsWith("."))

		val (files1, files2) = files.duplicate

		// Sorting file names by radical and storing groups for display purpose
		val sortedFiles = new HashMap[String, MSet[String]] with MultiMap[String, String]
		var sortedGroups = new TreeSet[String]

		for (fileName <- files1) {
			sortedFiles.addBinding(fileName.substring(0, fileName.indexOf("@")), fileName)
			sortedGroups += fileName.substring(0, fileName.indexOf("@"))
		}

		// We get the folder name of the run simulation
		if (!reportsOnly) {
			files2.size match {
				case 0 =>
					// If there is no simulation file
					logger.warn("There is no simulation script. Please verify that your scripts are in user-files/simulations and that they do not start with a .")
					sys.exit
				case 1 =>
					// If there is only one simulation file
					logger.info("There is only one simulation, executing it.")
					run(files2.next)
				case _ =>
					// If there are several simulation files
					println("Which simulation do you want to execute ?")

					var i = 0
					var filesList: List[String] = Nil

					for (group <- sortedGroups) {
						println("\n - " + group)
						sortedFiles.get(group).map {
							for (fileName <- _) {
								println("     [" + i + "] " + fileName.substring(fileName.indexOf("@") + 1, fileName.indexOf(".")))
								filesList = fileName :: filesList
								i += 1
							}
						}
					}

					println("\nSimulation #: ")

					val fileChosen = Console.readInt
					run(filesList.reverse(fileChosen))
			}
		} else {
			reportsOnlyFolder
		}
	}

	private def displayMenuAndRunForIde: String = {
		import CommandLineOptions.options._

		println("Which simulation do you want to execute ?")

		val files = Directory(ideSimulationFolder.get).files.map(_.name.takeWhile(_ != '.')).toSeq

		var i = 0
		files.foreach { file =>
			println("   [" + i + "] " + file)
			i += 1
		}

		val fileChosen = Console.readInt
		run(ideSimulationPackage.get + "." + files(fileChosen), true)
	}

	/**
	 * This method call the statistics module to generate the charts and statistics
	 *
	 * @param folderName The folder from which the simulation.log will be parsed
	 * @return Nothing
	 */
	private def generateStats(folderName: String) = ReportsGenerator.generateFor(folderName)

	/**
	 * This method actually runs the simulation by interpreting the scripts.
	 *
	 * @param fileName The name of the simulation file that will be executed
	 * @return The name of the folder of this simulation (ie: its date)
	 */
	private def run(fileName: String, isIde: Boolean = false) = {

		println("Simulation started...")

		val startDate = DateTime.now
		val interpreter =
			if (isIde)
				new IdeScenarioCompiler
			else
				fileName match {
					case fn if (fn.endsWith(".scala")) => new ScalaScenarioCompiler
					case fn if (fn.endsWith(".txt")) => new TextScenarioCompiler
					case _ => throw new UnsupportedOperationException
				}

		interpreter.run(fileName, startDate)

		// Returns the folderName in which the simulation is stored
		printFileNameDate(startDate)
	}
}

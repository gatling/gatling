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

import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ Set => MSet, MultiMap, HashMap }
import scala.tools.nsc.io.Directory

import org.joda.time.DateTime

import com.excilys.ebi.gatling.app.compiler.{ TextScenarioCompiler, ScalaScenarioCompiler, IdeScenarioCompiler }
import com.excilys.ebi.gatling.charts.report.ReportsGenerator
import com.excilys.ebi.gatling.core.config.GatlingFiles.{ resultFolder, GATLING_SIMULATIONS_FOLDER }
import com.excilys.ebi.gatling.core.config.GatlingConfig
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.DateHelper.printFileNameDate
import com.excilys.ebi.gatling.core.util.FileHelper.{ TXT_EXTENSION, SCALA_EXTENSION }
import com.excilys.ebi.gatling.core.Conventions

import CommandLineOptions.options.{ simulations, simulationPackage, simulationFolder, resultsFolder, requestBodiesFolder, reportsOnlyFolder, reportsOnly, noReports, dataFolder, configFileName }
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
			opt("sf", "simulations-folder", "<folderName>", "Uses <folderName> to discover simulations that could be run", { v: String => CommandLineOptions.setSimulationFolder(v) })
			opt("af", "assets-folder", "<folderName>", "Uses <assetsFolder> as folder for assets", { v: String => CommandLineOptions.setAssetsFolder(v) })
			opt("sp", "simulations-package", "<packageName>", "Uses <packageName> to start the simulations", { v: String => CommandLineOptions.setSimulationPackage(v) })
			opt("s", "simulations", "<simulationNames>", "Runs the <simulationNames> sequentially", { v: String => CommandLineOptions.setSimulations(v) })
		}

	/**
	 * Entry point of Application
	 *
	 * @param args Arguments of the main method
	 */
	def main(args: Array[String]) {

		if (cliOptsParser.parse(args)) {
			println("-----------\nGatling cli\n-----------\n")

			import CommandLineOptions.options._
			GatlingConfig(configFileName, dataFolder, requestBodiesFolder, resultsFolder, simulationFolder) // Initializes configuration

			// If simulations is set
			simulations.map { list =>
				if (list.size > 1)
					runMultipleSimulations(list)
				else
					run(list.head)
			}.getOrElse {
				if (!reportsOnly)
					// Else run with menu
					if (simulationPackage.isDefined) {
						val file = displayMenuForIde
						println(file)
						runForIde(file)
					} else
						run(displayMenu)
			}

			if (reportsOnly) {
				generateStats(reportsOnlyFolder)
			}
		}

		// if arguments are bad, usage message is displayed
	}

	def apply(dataFolder: String, resultsFolder: String, requestBodiesFolder: String, ideSimulationFolder: String, ideSimulationPackage: String) =
		main(Array("-df", dataFolder, "-rf", resultsFolder, "-bf", requestBodiesFolder, "-sf", ideSimulationFolder, "-sp", ideSimulationPackage))

	private def displayMenu: String = {
		import CommandLineOptions.options._

		// Getting files in scenarios folder
		val files = Directory(GATLING_SIMULATIONS_FOLDER).files.map(_.name).filter(name => name.endsWith(TXT_EXTENSION) || name.endsWith(SCALA_EXTENSION)).filterNot(_.startsWith("."))

		val (files1, files2) = files.duplicate

		// Sorting file names by radical and storing groups for display purpose
		val sortedFiles = new HashMap[String, MSet[String]] with MultiMap[String, String]
		var sortedGroups = new TreeSet[String]

		for (fileName <- files1) {
			Conventions.getSourceDirectoryNameFromRootFileName(fileName).map { sourceDirectoryName =>
				sortedFiles.addBinding(sourceDirectoryName, fileName)
				sortedGroups += sourceDirectoryName
			}
		}

		// We get the folder name of the run simulation
		files2.size match {
			case 0 =>
				// If there is no simulation file
				logger.warn("There are no simulation scripts. Please verify that your scripts are in user-files/simulations and that they do not start with a .")
				sys.exit
			case 1 =>
				// If there is only one simulation file
				logger.info("There is only one simulation, executing it.")
				files2.next
			case _ =>
				// If there are several simulation files
				println("Which simulation do you want to execute ?")

				var i = 0
				var filesList: List[String] = Nil

				for (group <- sortedGroups) {
					println("\n - " + group)
					sortedFiles.get(group).map {
						for (fileName <- _) {
							Conventions.getSimulationSpecificName(fileName).map { simulationSpecificName =>
								println("     [" + i + "] " + simulationSpecificName)
								filesList = fileName :: filesList
								i += 1
							}
						}
					}
				}

				println("\nSimulation #: ")

				val fileChosen = Console.readInt
				filesList.reverse(fileChosen)
		}
	}

	private def displayMenuForIde: String = {
		import CommandLineOptions.options._

		println("Which simulation do you want to execute ?")

		val files = Directory(GATLING_SIMULATIONS_FOLDER).files.map(_.name.takeWhile(_ != '.')).toSeq

		var i = 0
		files.foreach { file =>
			println("   [" + i + "] " + file)
			i += 1
		}

		val fileChosen = Console.readInt

		simulationPackage.get + "." + files(fileChosen)
	}

	/**
	 * This method call the statistics module to generate the charts and statistics
	 *
	 * @param folderName The folder from which the simulation.log will be parsed
	 * @return Nothing
	 */
	private def generateStats(folderName: String) = {
		println("Generating reports...")
		val start = currentTimeMillis
		ReportsGenerator.generateFor(folderName)
		println("Reports generated in " + (currentTimeMillis - start) / 1000 + "s.")
		println("Please go to the following directory : " + resultFolder(folderName))
	}

	/**
	 * This method actually runs the simulation by interpreting the scripts.
	 *
	 * @param fileName The name of the simulation file that will be executed
	 * @return The name of the folder of this simulation (ie: its date)
	 */
	private def run(fileName: String, isIde: Boolean = false) = {

		println("Simulation " + fileName + " started...")

		val startDate = DateTime.now
		val compiler =
			if (isIde)
				new IdeScenarioCompiler
			else
				fileName match {
					case fn if (fn.endsWith(".scala")) => new ScalaScenarioCompiler
					case fn if (fn.endsWith(".txt")) => new TextScenarioCompiler
					case _ => throw new UnsupportedOperationException
				}

		compiler.run(fileName, startDate)
		println("Simulation Finished.")

		// Returns the folderName in which the simulation is stored
		if (!noReports) {
			generateStats(printFileNameDate(startDate))
		}
	}

	private def runMultipleSimulations(fileNames: List[String]) = {
		val size = fileNames.size
		var count = 1

		fileNames.foreach { fileName =>
			println(">> Running simulation (" + count + "/" + size + ") - " + fileName)
			run(fileName)
			count += 1
		}
	}

	private def runForIde(fileName: String) = run(fileName, true)
}

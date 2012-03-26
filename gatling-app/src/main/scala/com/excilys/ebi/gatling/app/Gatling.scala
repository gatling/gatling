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
import java.io.{ StringWriter, PrintWriter }
import java.lang.System.currentTimeMillis

import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.Path.{ string2path, jfile2path }
import scala.tools.nsc.io.{ PlainFile, Path, File, Directory }
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.{ Settings, Global }

import org.joda.time.DateTime.now

import com.excilys.ebi.gatling.app.OptionsConstants.{ SIMULATIONS_OPTION, SIMULATIONS_FOLDER_OPTION, SIMULATIONS_FOLDER_ALIAS, SIMULATIONS_ALIAS, RESULTS_FOLDER_OPTION, RESULTS_FOLDER_ALIAS, REQUEST_BODIES_FOLDER_OPTION, REQUEST_BODIES_FOLDER_ALIAS, REPORTS_ONLY_OPTION, REPORTS_ONLY_ALIAS, NO_REPORTS_OPTION, NO_REPORTS_ALIAS, DATA_FOLDER_OPTION, DATA_FOLDER_ALIAS, CONFIG_FILE_OPTION, CONFIG_FILE_ALIAS, SIMULATIONS_BINARIES_FOLDER_OPTION, SIMULATIONS_BINARIES_FOLDER_ALIAS }
import com.excilys.ebi.gatling.app.UserSelection.DEFAULT_RUN_ID
import com.excilys.ebi.gatling.charts.config.ChartsFiles.activeSessionsFile
import com.excilys.ebi.gatling.charts.report.ReportsGenerator
import com.excilys.ebi.gatling.core.config.{ GatlingFiles, GatlingConfiguration }
import com.excilys.ebi.gatling.core.resource.ResourceRegistry
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.scenario.configuration.Simulation
import com.excilys.ebi.gatling.core.util.IOHelper.use
import com.twitter.io.TempDirectory

import akka.actor.Actor.registry
import grizzled.slf4j.Logging
import scopt.OptionParser
import com.excilys.ebi.gatling.app.Gatling.useActorSystem

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

		val cliOptions: Options = Options()

		val cliOptsParser = new OptionParser("gatling") {
			opt(NO_REPORTS_OPTION, NO_REPORTS_ALIAS, "Runs simulation but does not generate reports", { cliOptions.noReports = true })
			opt(REPORTS_ONLY_OPTION, REPORTS_ONLY_ALIAS, "<folderName>", "Generates the reports for the simulation in <folderName>", { v: String => cliOptions.reportsOnlyFolder = Some(v) })
			opt(CONFIG_FILE_OPTION, CONFIG_FILE_ALIAS, "<fileName>", "Uses <fileName> as the configuration file", { v: String => cliOptions.configFileName = Some(v) })
			opt(DATA_FOLDER_OPTION, DATA_FOLDER_ALIAS, "<folderName>", "Uses <folderName> as the folder where feeders are stored", { v: String => cliOptions.dataFolder = Some(v) })
			opt(RESULTS_FOLDER_OPTION, RESULTS_FOLDER_ALIAS, "<folderName>", "Uses <folderName> as the folder where results are stored", { v: String => cliOptions.resultsFolder = Some(v) })
			opt(REQUEST_BODIES_FOLDER_OPTION, REQUEST_BODIES_FOLDER_ALIAS, "<folderName>", "Uses <folderName> as the folder where request bodies are stored", { v: String => cliOptions.requestBodiesFolder = Some(v) })
			opt(SIMULATIONS_FOLDER_OPTION, SIMULATIONS_FOLDER_ALIAS, "<folderName>", "Uses <folderName> to discover simulations that could be run", { v: String => cliOptions.simulationSourcesFolder = Some(v) })
			opt(SIMULATIONS_BINARIES_FOLDER_OPTION, SIMULATIONS_BINARIES_FOLDER_ALIAS, "<folderName>", "Uses <folderName> to already compiled simulations", { v: String => cliOptions.simulationBinariesFolder = Some(v) })
			opt(SIMULATIONS_OPTION, SIMULATIONS_ALIAS, "<simulationNames>", "Runs the <simulationNames> sequentially", { v: String => cliOptions.simulations = Some(v.split(",").toList) })
		}

		// if arguments are incorrect, usage message is displayed
		if (cliOptsParser.parse(args))
			new Gatling(cliOptions).start
	}

	def useActorSystem[T](block: => T): T = {
		try {
			block

		} finally {
			// shut all actors down
			registry.shutdownAll

			// closes all the resources used during simulation
			ResourceRegistry.closeAll
		}
	}
}

class Gatling(cliOptions: Options) extends Logging {

	lazy val tempDir = Directory(TempDirectory.create(true))

	// Initializes configuration
	GatlingConfiguration.setUp(cliOptions.configFileName, cliOptions.dataFolder, cliOptions.requestBodiesFolder, cliOptions.resultsFolder, cliOptions.simulationSourcesFolder)

	def start {
		val runUuids = cliOptions.reportsOnlyFolder match {
			case Some(reportsOnlyFolder) => List(reportsOnlyFolder)
			case None =>
				val classes = cliOptions.simulationBinariesFolder match {

					case Some(simulationBinariesFolder) =>
						// expect simulations to have been pre-compiled (ex: IDE)
						val classNames = getClassNamesFromBinariesDirectory(Directory(simulationBinariesFolder))
						loadSimulationClasses(classNames)

					case None =>
						val scalaFiles = collectFiles(GatlingFiles.simulationsFolder, "scala")
						val classloader = compile(scalaFiles)
						val classNames = getClassNamesFromBinariesDirectory(tempDir)
						loadSimulationClasses(classNames, classloader)
				}

				val userSelection = cliOptions.simulations match {
					case Some(simulations) => autoSelect(classes, simulations)
					case None => interactiveSelect(classes)
				}

				run(userSelection)
		}

		if (!cliOptions.noReports)
			runUuids.foreach(generateReports)
	}

	private def autoSelect(classes: List[Class[Simulation]], simulations: List[String]): UserSelection = UserSelection(classes.filter(clazz => simulations.contains(clazz.getName)))

	private def interactiveSelect(classes: List[Class[Simulation]]): UserSelection = {

		val simulation = selectSimulationClass(classes)

		println("Select run id (default is '" + DEFAULT_RUN_ID + "'). Accepted characters are a-z, A-Z, 0-9, - and _")
		val runId = {
			val userInput = Console.readLine.trim
			if (userInput.isEmpty) DEFAULT_RUN_ID else userInput
		}

		if (!runId.matches("[\\w-_]*"))
			throw new IllegalArgumentException(runId + " contains illegal characters")

		println("Select run description (optional)")
		val runDescription = Console.readLine.trim

		UserSelection(List(simulation), runId, runDescription)
	}

	private def collectFiles(directory: Path, extension: String): List[File] = Directory(directory).deepFiles.filter(_.hasExtension(extension)).toList

	private def compile(files: List[File]): AbstractFileClassLoader = {

		println("Collecting simulations...")

		val byteCodeDir = PlainFile.fromPath(tempDir)
		val classLoader = new AbstractFileClassLoader(byteCodeDir, getClass.getClassLoader)

		val settings = new Settings
		settings.usejavacp.value = true
		settings.outputDirs.setSingleOutput(byteCodeDir)
		settings.deprecation.value = true
		settings.unchecked.value = true

		// Prepare an object for collecting error messages from the compiler
		val messageCollector = new StringWriter

		use(new PrintWriter(messageCollector)) { pw =>
			// Initialize the compiler
			val reporter = new ConsoleReporter(settings, Console.in, pw)
			val compiler = new Global(settings, reporter)

			(new compiler.Run).compileFiles(files.map(PlainFile.fromPath(_)))

			// Bail out if compilation failed
			if (reporter.hasErrors) {
				reporter.printSummary
				throw new RuntimeException("Compilation failed:\n" + messageCollector.toString)
			}

			classLoader
		}
	}

	private def pathToClassName(path: Path, root: Path): String = (path.parent / path.stripExtension)
		.toString
		.stripPrefix(root + File.separator)
		.replace(File.separator, ".")

	private def getClassNamesFromBinariesDirectory(dir: Directory): List[String] = dir
		.deepFiles
		.filter(_.hasExtension("class"))
		.map(pathToClassName(_, dir)).toList

	private def loadSimulationClasses(classNames: List[String]): List[Class[Simulation]] = classNames
		.map(Class.forName(_))
		.filter(classOf[Simulation].isAssignableFrom(_))
		.map(_.asInstanceOf[Class[Simulation]]).toList

	private def loadSimulationClasses(classNames: List[String], classLoader: AbstractFileClassLoader): List[Class[Simulation]] = classNames
		.map(classLoader.findClass(_))
		.filter(classOf[Simulation].isAssignableFrom(_))
		.map(_.asInstanceOf[Class[Simulation]]).toList

	private def selectSimulationClass(classes: List[Class[Simulation]]): Class[Simulation] = {

		val selection = classes.size match {
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
					println("     [" + i + "] " + classes(i).getName)
				}
				Console.readInt
		}

		classes(selection)
	}

	private def run(selection: UserSelection): Seq[String] = {

		val size = selection.simulationClasses.size

		useActorSystem {
			for (i <- 0 until size) yield {
				val simulationClass = selection.simulationClasses(i)
				val name = simulationClass.getName
				println(">> Running simulation (" + (i + 1) + "/" + size + ") - " + name)
				println("Simulation " + name + " started...")

				val runInfo = new RunRecord(now, selection.runId, selection.runDescription)

				val simulation = simulationClass.newInstance
				val configurations = simulation()
				new Runner(runInfo, configurations).run

				println("Simulation Finished.")
				runInfo.runUuid
			}
		}
	}

	/**
	 * This method call the statistics module to generate the charts and statistics
	 *
	 * @param folderName The folder from which the simulation.log will be parsed
	 * @return Nothing
	 */
	private def generateReports(runUuid: String) {
		println("Generating reports...")
		val start = currentTimeMillis
		if (ReportsGenerator.generateFor(runUuid)) {
			println("Reports generated in " + (currentTimeMillis - start) / 1000 + "s.")
			println("Please open the following file : " + activeSessionsFile(runUuid))
		} else {
			println("Reports weren't generated")
		}
	}
}

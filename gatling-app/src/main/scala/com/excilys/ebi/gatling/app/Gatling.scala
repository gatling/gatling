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

import java.io.{ PrintWriter, StringWriter }
import java.lang.System.currentTimeMillis
import java.lang.reflect.Modifier

import scala.Array.canBuildFrom
import scala.tools.nsc.{ Global, Settings }
import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.{ Directory, File, Path }
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.PlainFile
import scala.tools.nsc.reporters.ConsoleReporter

import org.joda.time.DateTime.now

import com.excilys.ebi.gatling.app.OptionsConstants._
import com.excilys.ebi.gatling.charts.config.ChartsFiles.globalFile
import com.excilys.ebi.gatling.charts.report.ReportsGenerator
import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.{ GatlingConfiguration, GatlingFiles, GatlingOptions }
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.scenario.configuration.Simulation
import com.excilys.ebi.gatling.core.util.FileHelper
import com.excilys.ebi.gatling.core.util.IOHelper.use

import grizzled.slf4j.Logging
import scopt.OptionParser

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

		val gatlingOptions = GatlingOptions()

		val cliOptsParser = new OptionParser("gatling") {
			opt(NO_REPORTS_OPTION, NO_REPORTS_ALIAS, "Runs simulation but does not generate reports", { gatlingOptions.noReports = true })
			opt(REPORTS_ONLY_OPTION, REPORTS_ONLY_ALIAS, "<directoryName>", "Generates the reports for the simulation in <directoryName>", { v: String => gatlingOptions.reportsOnlyDirectoryName = Some(v) })
			opt(CONFIG_FILE_OPTION, CONFIG_FILE_ALIAS, "<file>", "Uses <file> as the configuration file", { v: String => gatlingOptions.configFilePath = Some(v) })
			opt(DATA_FOLDER_OPTION, DATA_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> as the absolute path of the directory where feeders are stored", { v: String => gatlingOptions.dataDirectoryPath = Some(v) })
			opt(RESULTS_FOLDER_OPTION, RESULTS_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> as the absolute path of the directory where results are stored", { v: String => gatlingOptions.resultsDirectoryPath = Some(v) })
			opt(REQUEST_BODIES_FOLDER_OPTION, REQUEST_BODIES_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> as the absolute path of the directory where request bodies are stored", { v: String => gatlingOptions.requestBodiesDirectoryPath = Some(v) })
			opt(SIMULATIONS_FOLDER_OPTION, SIMULATIONS_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> to discover simulations that could be run", { v: String => gatlingOptions.simulationSourcesDirectoryPath = Some(v) })
			opt(SIMULATIONS_BINARIES_FOLDER_OPTION, SIMULATIONS_BINARIES_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> to discover already compiled simulations", { v: String => gatlingOptions.simulationBinariesDirectoryPath = Some(v) })
			opt(SIMULATIONS_OPTION, SIMULATIONS_ALIAS, "<classNamesList>", "Runs the <classNamesList> simulations sequentially", { v: String => gatlingOptions.simulationClassNames = Some(v.split(",").map(_.trim).toList) })
			opt(RUN_NAME_OPTION, RUN_NAME_ALIAS, "<runName>", "Use <runName> for the output directory", { v: String => gatlingOptions.runName = v })
		}

		// if arguments are incorrect, usage message is displayed
		if (cliOptsParser.parse(args))
			new Gatling(gatlingOptions).start
	}

	def useActorSystem[T](block: => T): T = {
		try {
			block

		} finally {
			// shut all actors down
			system.shutdown
		}
	}
}

class Gatling(options: GatlingOptions) extends Logging {

	lazy val tempDir = Directory(FileHelper.createTempDirectory())

	// Initializes configuration
	GatlingConfiguration.setUp(options)

	def start {
		val runUuids = options.reportsOnlyDirectoryName
			.map(List(_))
			.getOrElse {
				val classes = options.simulationBinariesDirectoryPath.map { simulationBinariesDirectoryPath =>
					// expect simulations to have been pre-compiled (ex: IDE)
					val classNames = getClassNamesFromBinariesDirectory(simulationBinariesDirectoryPath.toDirectory)
					loadSimulationClasses(classNames)

				}.getOrElse {
					val scalaFiles = collectFiles(GatlingFiles.simulationSourcesDirectory, "scala")
					val classloader = compile(scalaFiles)
					val classNames = getClassNamesFromBinariesDirectory(tempDir)
					loadSimulationClasses(classNames, classloader)
				}

				val userSelection = options.simulationClassNames.map { simulations =>
					autoSelect(classes, simulations, options)
				}.getOrElse {
					interactiveSelect(classes, options)
				}

				run(userSelection)
			}

		if (!options.noReports)
			runUuids.foreach(generateReports)
	}

	private def autoSelect(classes: List[Class[Simulation]], simulations: List[String], options: GatlingOptions): UserSelection = {

		val classNames = classes.map(_.getName)
		val notFounds = simulations.filterNot(classNames.contains(_))
		if (!notFounds.isEmpty)
			println("The following simulation names didn't match any Simulation class name and were filtered out: " + notFounds)

		val runId = options.runName
		val runDescription = "run"

		UserSelection(classes.filter(clazz => simulations.contains(clazz.getName)), runId, runDescription)
	}

	private def interactiveSelect(classes: List[Class[Simulation]], options: GatlingOptions): UserSelection = {

		val simulation = selectSimulationClass(classes)

		println("Select run id (default is '" + GatlingOptions.DEFAULT_RUN_ID + "'). Accepted characters are a-z, A-Z, 0-9, - and _")
		val runId = {
			val userInput = Console.readLine.trim

			if (!userInput.matches("[\\w-_]*"))
				throw new IllegalArgumentException(userInput + " contains illegal characters")

			if (!userInput.isEmpty) userInput else options.runName
		}

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
		settings.encoding.value = GatlingConfiguration.configuration.encoding

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

	private def filterSimulationClasses(classes: List[Class[_]]) = classes
		.filter { clazz => classOf[Simulation].isAssignableFrom(clazz) && !clazz.isInterface && !Modifier.isAbstract(clazz.getModifiers) }
		.map(_.asInstanceOf[Class[Simulation]])

	private def loadSimulationClasses(classNames: List[String]): List[Class[Simulation]] = filterSimulationClasses(classNames.map(Class.forName(_)))

	private def loadSimulationClasses(classNames: List[String], classLoader: AbstractFileClassLoader): List[Class[Simulation]] = filterSimulationClasses(classNames.map(classLoader.findClass(_)))

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

		val validRange = 0 until classes.size
		if (validRange contains selection)
			classes(selection)
		else {
			println("Invalid selection, must be in " + validRange)
			selectSimulationClass(classes)
		}
	}

	private def run(selection: UserSelection): Seq[String] = {

		val size = selection.simulationClasses.size

		Gatling.useActorSystem {
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
	 * @param runUuid The directory from which the simulation.log will be parsed
	 * @return Nothing
	 */
	private def generateReports(runUuid: String) {
		println("Generating reports...")
		val start = currentTimeMillis
		try {
			ReportsGenerator.generateFor(runUuid)
			println("Reports generated in " + (currentTimeMillis - start) / 1000 + "s.")
			println("Please open the following file : " + globalFile(runUuid))

		} catch {
			case e => error("Reports weren't generated", e)
		}
	}
}

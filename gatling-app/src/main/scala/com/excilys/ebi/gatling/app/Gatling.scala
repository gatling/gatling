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

import com.excilys.ebi.gatling.app.UserSelection.DEFAULT_RUN_ID
import com.excilys.ebi.gatling.charts.config.ChartsFiles.activeSessionsFile
import com.excilys.ebi.gatling.charts.report.ReportsGenerator
import com.excilys.ebi.gatling.core.config.{ GatlingFiles, GatlingConfiguration }
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.util.IOHelper.use
import com.twitter.io.TempDirectory

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

		val options: Options = Options()

		val cliOptsParser = new OptionParser("gatling") {
			opt("nr", "no-reports", "Runs simulation but does not generate reports", { options.noReports = true })
			opt("ro", "reports-only", "<folderName>", "Generates the reports for the simulation in <folderName>", { v: String => options.reportsOnlyFolder = Some(v) })
			opt("cf", "config-file", "<fileName>", "Uses <fileName> as the configuration file", { v: String => options.configFileName = Some(v) })
			opt("df", "data-folder", "<folderName>", "Uses <folderName> as the folder where feeders are stored", { v: String => options.dataFolder = Some(v) })
			opt("rf", "results-folder", "<folderName>", "Uses <folderName> as the folder where results are stored", { v: String => options.resultsFolder = Some(v) })
			opt("bf", "request-bodies-folder", "<folderName>", "Uses <folderName> as the folder where request bodies are stored", { v: String => options.requestBodiesFolder = Some(v) })
			opt("sf", "simulations-folder", "<folderName>", "Uses <folderName> to discover simulations that could be run", { v: String => options.simulationSourcesFolder = Some(v) })
			opt("s", "simulations", "<simulationNames>", "Runs the <simulationNames> sequentially", { v: String => options.simulations = Some(v.split(",").toList) })
		}

		if (cliOptsParser.parse(args))
			start(options)

		// if arguments are incorrect, usage message is displayed
	}

	def start(options: Options) = new Gatling(options: Options).launch
}

class Gatling(options: Options) extends Logging {

	lazy val tempDir = Directory(TempDirectory.create(true))

	// Initializes configuration
	GatlingConfiguration.setUp(options.configFileName, options.dataFolder, options.requestBodiesFolder, options.resultsFolder, options.simulationSourcesFolder)

	def launch {
		val runUuids = options.reportsOnlyFolder match {
			case Some(reportsOnlyFolder) => List(reportsOnlyFolder)
			case None =>
				val classes = options.simulationBinariesFolder match {

					case Some(simulationBinariesFolder) =>
						// expect simulations to have been pre-compiled (ex: IDE)
						val classNames = getClassNamesFromBinariesDirectory(Directory(simulationBinariesFolder))
						loadSimulationClasses(classNames)

					case None =>
						// TODO use andThen once I f***g manage to use it with ScalaIDE
						val scalaFiles = collectFiles(GatlingFiles.simulationsFolder, "scala")
						val classloader = compile(scalaFiles)
						val classNames = getClassNamesFromBinariesDirectory(tempDir)
						loadSimulationClasses(classNames, classloader)
				}

				val userSelection = options.simulations match {
					case Some(simulations) => silentSelect(classes, simulations)
					case None => interactiveSelect(classes)
				}

				run(userSelection)
		}

		if (!options.noReports)
			runUuids.foreach(generateReports(_))
	}

	private def silentSelect(classes: List[Class[Simulation]], simulations: List[String]): UserSelection = UserSelection(classes.filter(clazz => simulations.contains(clazz.getName)))

	private def interactiveSelect(classes: List[Class[Simulation]]): UserSelection = {
		val simulation = selectSimulationClass(classes)

		println("Select run id (required, default is '" + DEFAULT_RUN_ID + "'). Accepted characters are a-z, A-Z, 0-9, - and _")
		val userRunId = Console.readLine.trim
		val runId = if (userRunId.isEmpty) DEFAULT_RUN_ID else userRunId

		if (!runId.matches("[\\w-_]*"))
			throw new IllegalArgumentException(runId + " contains illegal characters")

		println("Select run name (optional)")
		val runName = Console.readLine.trim

		UserSelection(List(simulation), runId, if (runName.isEmpty) runId else runName)
	}

	private def collectFiles(directory: Path, extension: String): List[File] = Directory(directory).deepFiles.filter(_.hasExtension(extension)).toList

	private def compile(files: List[File]): AbstractFileClassLoader = {

		val byteCodeDir = PlainFile.fromPath(tempDir)
		val classLoader = new AbstractFileClassLoader(byteCodeDir, getClass.getClassLoader)

		def generateSettings: Settings = {
			val settings = new Settings
			settings.usejavacp.value = true
			settings.outputDirs.setSingleOutput(byteCodeDir)
			settings.deprecation.value = true
			settings.unchecked.value = true
			settings
		}

		// Prepare an object for collecting error messages from the compiler
		val messageCollector = new StringWriter

		use(new PrintWriter(messageCollector)) { pw =>
			// Initialize the compiler
			val settings = generateSettings
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

	private def getClassNamesFromBinariesDirectory(dir: Directory): List[String] = dir.deepFiles
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

		val selected = classes.size match {
			case 0 =>
				// If there is no simulation file
				error("There are no simulation scripts. Please verify that your scripts are in user-files/simulations and that they do not start with a .")
				sys.exit
			case 1 =>
				// If there is only one simulation file
				info("There is only one simulation, executing it.")
				0
			case size =>
				for (i <- 0 until size) {
					println("     [" + i + "] " + classes(i).getName)
				}
				Console.readInt
		}

		classes(selected)
	}

	private def run(selection: UserSelection): Seq[String] = {

		val size = selection.simulationClasses.size

		for (i <- 0 until size) yield {
			val simulation = selection.simulationClasses(i)
			val name = simulation.getName
			println(">> Running simulation (" + (i + 1) + "/" + size + ") - " + name)
			println("Simulation " + name + " started...")

			val runInfo = new RunRecord(now, selection.runId, selection.runName)

			new Runner(runInfo, simulation.newInstance()()).run
			println("Simulation Finished.")

			runInfo.runUuid
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

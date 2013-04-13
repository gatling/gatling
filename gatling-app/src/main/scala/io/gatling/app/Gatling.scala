/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.app

import java.lang.System.currentTimeMillis

import scala.collection.mutable

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.app.CommandLineConstants._
import io.gatling.charts.report.ReportsGenerator
import io.gatling.core.config.{ GatlingFiles, GatlingPropertiesBuilder }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.reader.DataReader
import io.gatling.core.runner.{ Runner, Selection }
import io.gatling.core.scenario.configuration.Simulation
import io.gatling.core.structure.Assertion
import io.gatling.core.util.FileHelper.FileRichString
import scopt.OptionParser

/**
 * Object containing entry point of application
 */
object GatlingStatusCodes {
	val success = 0
	val invalidArguments = 1
	val assertionsFailed = 2
}

object Gatling {

	/**
	 * Entry point of Application
	 *
	 * @param args Arguments of the main method
	 */
	def main(args: Array[String]) {
		sys.exit(runGatling(args))
	}

	def fromMap(props: mutable.Map[String, Any]) = {
		GatlingConfiguration.setUp(props)
		new Gatling().start
	}

	def runGatling(args: Array[String]) = {
		val props = new GatlingPropertiesBuilder

		val cliOptsParser = new OptionParser("gatling") {
			help(HELP, HELP_ALIAS, "Show help (this message) and exit")
			opt(CLI_NO_REPORTS, CLI_NO_REPORTS_ALIAS, "Runs simulation but does not generate reports", props.noReports)
			opt(CLI_REPORTS_ONLY, CLI_REPORTS_ONLY_ALIAS, "<directoryName>", "Generates the reports for the simulation in <directoryName>", props.reportsOnly _)
			opt(CLI_DATA_FOLDER, CLI_DATA_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> as the absolute path of the directory where feeders are stored", props.dataDirectory _)
			opt(CLI_RESULTS_FOLDER, CLI_RESULTS_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> as the absolute path of the directory where results are stored", props.resultsDirectory _)
			opt(CLI_REQUEST_BODIES_FOLDER, CLI_REQUEST_BODIES_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> as the absolute path of the directory where request bodies are stored", props.requestBodiesDirectory _)
			opt(CLI_SIMULATIONS_FOLDER, CLI_SIMULATIONS_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> to discover simulations that could be run", props.sourcesDirectory _)
			opt(CLI_SIMULATIONS_BINARIES_FOLDER, CLI_SIMULATIONS_BINARIES_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> to discover already compiled simulations", props.binariesDirectory _)
			opt(CLI_SIMULATION, CLI_SIMULATION_ALIAS, "<className>", "Runs <className> simulation", props.simulationClass _)
			opt(CLI_OUTPUT_DIRECTORY_BASE_NAME, CLI_OUTPUT_DIRECTORY_BASE_NAME_ALIAS, "<name>", "Use <name> for the base name of the output directory", props.outputDirectoryBaseName _)
		}

		// if arguments are incorrect, usage message is displayed
		if (cliOptsParser.parse(args)) fromMap(props.build)
		else GatlingStatusCodes.invalidArguments
	}

}

class Gatling extends Logging {

	def start = {

		def defaultOutputDirectoryBaseName(clazz: Class[Simulation]) = configuration.core.outputDirectoryBaseName.getOrElse(clazz.getSimpleName.toFilename)

		def getSingleSimulation(simulations: List[Class[Simulation]]) = configuration.core.simulationClass.map(_ => simulations.head.newInstance)

		def interactiveSelect(simulations: List[Class[Simulation]]): Selection = {

			def selectSimulationClass(simulations: List[Class[Simulation]]): Class[Simulation] = {

				val selection = simulations.size match {
					case 0 =>
						// If there is no simulation file
						println("There is no simulation script. Please check that your scripts are in user-files/simulations")
						sys.exit
					case 1 =>
						println(s"${simulations.head.getName} is the only simulation, executing it.")
						0
					case size =>
						println("Choose a simulation number:")
						for ((simulation, index) <- simulations.zipWithIndex) {
							println(s"     [$index] ${simulation.getName}")
						}
						Console.readInt
				}

				val validRange = 0 until simulations.size
				if (validRange contains selection)
					simulations(selection)
				else {
					println(s"Invalid selection, must be in $validRange")
					selectSimulationClass(simulations)
				}
			}

			val simulation = selectSimulationClass(simulations)

			val myDefaultOutputDirectoryBaseName = defaultOutputDirectoryBaseName(simulation)

			println(s"Select simulation id (default is '$myDefaultOutputDirectoryBaseName'). Accepted characters are a-z, A-Z, 0-9, - and _")
			val simulationId = {
				val userInput = Console.readLine.trim

				require(userInput.matches("[\\w-_]*"), s"$userInput contains illegal characters")

				if (!userInput.isEmpty) userInput else myDefaultOutputDirectoryBaseName
			}

			println("Select run description (optional)")
			val runDescription = Console.readLine.trim

			new Selection(simulation, simulationId, runDescription)
		}

		def applyAssertions(simulation: Simulation, dataReader: DataReader) = {
			val successful = Assertion.assertThat(simulation.assertions, dataReader)

			if (successful) {
				println("Simulation successful.")
				GatlingStatusCodes.success
			} else {
				println("Simulation failed.")
				GatlingStatusCodes.assertionsFailed
			}
		}

		def generateReports(outputDirectoryName: String, dataReader: => DataReader) {
			println("Generating reports...")
			val start = currentTimeMillis
			val indexFile = ReportsGenerator.generateFor(outputDirectoryName, dataReader)
			println(s"Reports generated in ${(currentTimeMillis - start) / 1000}s.")
			println(s"Please open the following file : $indexFile")
		}

		def attemptDirectSimulationClassLoading(className: String): Option[List[Class[Simulation]]] = try {
			val clazz = getClass.getClassLoader.loadClass(className).asInstanceOf[Class[Simulation]]
			Some(List(clazz))
		} catch {
			case e: ClassNotFoundException =>
				logger.info(s"Could not find simulation class $className from class loader, will try to compile it from sources")

				None
			case e: Exception =>
				println(s"Could not properly load simulation class $className")
				throw e
		}

		def regularSimulationClassLoading: List[Class[Simulation]] = {
			val simulationClassLoader = GatlingFiles.binariesDirectory
				.map(SimulationClassLoader.fromClasspathBinariesDirectory) // expect simulations to have been pre-compiled (ex: IDE)
				.getOrElse(SimulationClassLoader.fromSourcesDirectory(GatlingFiles.sourcesDirectory))

			simulationClassLoader
				.simulationClasses(configuration.core.simulationClass)
				.sortWith(_.getName < _.getName)
		}

		val simulations = configuration.core.simulationClass
			.flatMap(attemptDirectSimulationClassLoading)
			.getOrElse(regularSimulationClassLoading)

		val (outputDirectoryName, simulation) = GatlingFiles.reportsOnlyDirectory
			.map((_, getSingleSimulation(simulations)))
			.getOrElse {
				val selection = configuration.core.simulationClass.map { _ =>
					val simulation = simulations.head
					val outputDirectoryBaseName = defaultOutputDirectoryBaseName(simulation)
					new Selection(simulation, outputDirectoryBaseName, outputDirectoryBaseName)
				}.getOrElse(interactiveSelect(simulations))

				val (runId, simulation) = new Runner(selection).run
				(runId, Some(simulation))
			}

		lazy val dataReader = DataReader.newInstance(outputDirectoryName)

		val result = simulation match {
			case Some(simulation) if !simulation.assertions.isEmpty => applyAssertions(simulation, dataReader)
			case _ => GatlingStatusCodes.success
		}

		if (!configuration.charting.noReports) generateReports(outputDirectoryName, dataReader)

		result
	}
}

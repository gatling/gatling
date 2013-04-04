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

import io.gatling.app.CommandLineConstants._
import io.gatling.charts.report.ReportsGenerator
import io.gatling.core.config.{ GatlingConfiguration, GatlingFiles, GatlingPropertiesBuilder }
import io.gatling.core.result.reader.DataReader
import io.gatling.core.runner.{ Runner, Selection }
import io.gatling.core.scenario.configuration.Simulation
import io.gatling.core.structure.Assertion
import io.gatling.core.util.FileHelper.formatToFilename

import scopt.OptionParser

/**
 * Object containing entry point of application
 */
object Gatling {

	val SUCCESS = 0
	val INCORRECT_ARGUMENTS = 1
	val SIMULATION_ASSERTIONS_FAILED = 2

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
			opt(CLI_SIMULATION, CLI_SIMULATION_ALIAS, "<className>", "Runs <className> simulation", props.clazz _)
			opt(CLI_OUTPUT_DIRECTORY_BASE_NAME, CLI_OUTPUT_DIRECTORY_BASE_NAME_ALIAS, "<name>", "Use <name> for the base name of the output directory", props.outputDirectoryBaseName _)
		}

		// if arguments are incorrect, usage message is displayed
		if (cliOptsParser.parse(args)) fromMap(props.build)
		else INCORRECT_ARGUMENTS
	}

}

class Gatling {

	import GatlingConfiguration.configuration

	private def defaultOutputDirectoryBaseName(clazz: Class[Simulation]) = configuration.simulation.outputDirectoryBaseName.getOrElse(formatToFilename(clazz.getSimpleName))

	def start = {
		val simulations = GatlingFiles.binariesDirectory
			.map(SimulationClassLoader.fromClasspathBinariesDirectory) // expect simulations to have been pre-compiled (ex: IDE)
			.getOrElse(SimulationClassLoader.fromSourcesDirectory(GatlingFiles.sourcesDirectory))
			.simulationClasses(configuration.simulation.clazz)
			.sortWith(_.getName < _.getName)

		val (outputDirectoryName, simulation) = GatlingFiles.reportsOnlyDirectory.map((_, getSingleSimulation(simulations)))
			.getOrElse {
				val selection = configuration.simulation.clazz.map { _ =>
					val simulation = simulations.head
					val outputDirectoryBaseName = defaultOutputDirectoryBaseName(simulation)
					new Selection(simulation, outputDirectoryBaseName, outputDirectoryBaseName)
				}.getOrElse(interactiveSelect(simulations))

			val (runId, simulation) = new Runner(selection).run
			(runId, Some(simulation))
		}

		lazy val dataReader = DataReader.newInstance(outputDirectoryName)

		val result = simulation match {
			case Some(simulation) if !simulation.assertions.isEmpty => if (applyAssertions(simulation, dataReader)) Gatling.SUCCESS else Gatling.SIMULATION_ASSERTIONS_FAILED
			case None => Gatling.SUCCESS
		}

		if (!configuration.charting.noReports) generateReports(outputDirectoryName, dataReader)

		result
	}

	private def getSingleSimulation(simulations: List[Class[Simulation]]) =
		configuration.simulation.clazz.map(_ => simulations.head.newInstance)

	private def interactiveSelect(simulations: List[Class[Simulation]]): Selection = {

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

	private def selectSimulationClass(simulations: List[Class[Simulation]]): Class[Simulation] = {

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

	/**
	 * This method call the statistics module to generate the charts and statistics
	 *
	 * @param outputDirectoryName The directory from which the simulation.log will be parsed
	 */
	private def generateReports(outputDirectoryName: String, dataReader: => DataReader) {
		println("Generating reports...")
		val start = currentTimeMillis
		val indexFile = ReportsGenerator.generateFor(outputDirectoryName, dataReader)
		println(s"Reports generated in ${(currentTimeMillis - start) / 1000}s.")
		println(s"Please open the following file : $indexFile")
	}

	private def applyAssertions(simulation: Simulation, dataReader: DataReader) = {
		val successful = Assertion.assertThat(simulation.assertions, dataReader)

		if (successful) println("Simulation successful.")
		else println("Simulation failed.")

		successful
	}
}

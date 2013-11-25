/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import scala.util.Try

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.app.CommandLineConstants._
import io.gatling.charts.report.ReportsGenerator
import io.gatling.core.assertion.Assertion
import io.gatling.core.config.{ GatlingFiles, GatlingPropertiesBuilder }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.reader.DataReader
import io.gatling.core.runner.{ Runner, Selection }
import io.gatling.core.scenario.Simulation
import io.gatling.core.util.StringHelper.RichString
import scopt.OptionParser

/**
 * Object containing entry point of application
 */
object Gatling {

	/**
	 * Entry point of Application
	 *
	 * @param args Arguments of the main method
	 */
	def main(args: Array[String]) {
		sys.exit(runGatling(args))
	}

	def fromMap(props: mutable.Map[String, Any], simulationClass: Option[Class[Simulation]] = None) = {
		GatlingConfiguration.setUp(props)
		new Gatling(simulationClass).start
	}

	def runGatling(args: Array[String], simulationClass: Option[Class[Simulation]] = None) = {
		val props = new GatlingPropertiesBuilder

		val cliOptsParser = new OptionParser[Unit]("gatling") {
			help(HELP).abbr(HELP_SHORT).text("Show help (this message) and exit")
			opt[Unit](NO_REPORTS).abbr(NO_REPORTS_SHORT).foreach(_ => props.noReports).text("Runs simulation but does not generate reports")
			opt[String](REPORTS_ONLY).abbr(REPORTS_ONLY_SHORT).foreach(props.reportsOnly).valueName("<directoryName>").text("Generates the reports for the simulation in <directoryName>")
			opt[String](DATA_FOLDER).abbr(DATA_FOLDER_SHORT).foreach(props.dataDirectory).valueName("<directoryPath>").text("Uses <directoryPath> as the absolute path of the directory where feeders are stored")
			opt[String](RESULTS_FOLDER).abbr(RESULTS_FOLDER_SHORT).foreach(props.resultsDirectory).valueName("<directoryPath>").text("Uses <directoryPath> as the absolute path of the directory where results are stored")
			opt[String](REQUEST_BODIES_FOLDER).abbr(REQUEST_BODIES_FOLDER_SHORT).foreach(props.requestBodiesDirectory).valueName("<directoryPath>").text("Uses <directoryPath> as the absolute path of the directory where request bodies are stored")
			opt[String](SIMULATIONS_FOLDER).abbr(SIMULATIONS_FOLDER_SHORT).foreach(props.sourcesDirectory).valueName("<directoryPath>").text("Uses <directoryPath> to discover simulations that could be run")
			opt[String](SIMULATIONS_BINARIES_FOLDER).abbr(SIMULATIONS_BINARIES_FOLDER_SHORT).foreach(props.binariesDirectory).valueName("<directoryPath>").text("Uses <directoryPath> to discover already compiled simulations")
			opt[String](SIMULATION).abbr(SIMULATION_SHORT).foreach(props.simulationClass).valueName("<className>").text("Runs <className> simulation")
			opt[String](OUTPUT_DIRECTORY_BASE_NAME).abbr(OUTPUT_DIRECTORY_BASE_NAME_SHORT).foreach(props.outputDirectoryBaseName).valueName("<name>").text("Use <name> for the base name of the output directory")
			opt[String](SIMULATION_DESCRIPTION).abbr(SIMULATION_DESCRIPTION_SHORT).foreach(props.runDescription).valueName("<description>").text("A short <description> of the run to include in the report")
		}

		// if arguments are incorrect, usage message is displayed
		if (cliOptsParser.parse(args)) fromMap(props.build, simulationClass)
		else GatlingStatusCodes.invalidArguments
	}

}

class Gatling(simulationClass: Option[Class[Simulation]]) extends Logging {

	def start = {

		def defaultOutputDirectoryBaseName(clazz: Class[Simulation]) = configuration.core.outputDirectoryBaseName.getOrElse(clazz.getSimpleName.clean)

		def getSingleSimulation(simulations: List[Class[Simulation]]) = configuration.core.simulationClass.map(_ => simulations.head.newInstance)

		def interactiveSelect(simulations: List[Class[Simulation]]): Selection = {

			def selectSimulationClass(simulations: List[Class[Simulation]]): Class[Simulation] = {

				def readSimulationNumber: Int =
					Try(Console.readInt).getOrElse {
						println("Invalid characters, please provide a correct simulation number:")
						readSimulationNumber
					}

				val selection = simulations.size match {
					case 0 =>
						// If there is no simulation file
						println("There is no simulation script. Please check that your scripts are in user-files/simulations")
						sys.exit
					case 1 =>
						println(s"${simulations.head.getName} is the only simulation, executing it.")
						0
					case _ =>
						println("Choose a simulation number:")
						for ((simulation, index) <- simulations.zipWithIndex) {
							println(s"     [$index] ${simulation.getName}")
						}
						readSimulationNumber
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

		val simulations = simulationClass.map(List(_)).getOrElse {
			if (configuration.core.disableCompiler) {
				configuration.core.simulationClass
					.map(clazz => List(Class.forName(clazz).asInstanceOf[Class[Simulation]]))
					.getOrElse(throw new IllegalArgumentException("Compiler is disable, but no simulation class is specified"))

			} else {
				val simulationClassLoader = GatlingFiles.binariesDirectory
					.map(SimulationClassLoader.fromClasspathBinariesDirectory) // expect simulations to have been pre-compiled (ex: IDE)
					.getOrElse(SimulationClassLoader.fromSourcesDirectory(GatlingFiles.sourcesDirectory))

				simulationClassLoader
					.simulationClasses(configuration.core.simulationClass)
					.sortBy(_.getName)
			}
		}

		val (outputDirectoryName, simulation) = GatlingFiles.reportsOnlyDirectory match {
			case Some(dir) =>
				(dir, getSingleSimulation(simulations))

			case None =>
				val selection = configuration.core.simulationClass match {
					case Some(_) =>
						// FIXME ugly
						val simulation = simulations.head
						val outputDirectoryBaseName = defaultOutputDirectoryBaseName(simulation)
						val runDescription = configuration.core.runDescription.getOrElse(outputDirectoryBaseName)
						new Selection(simulation, outputDirectoryBaseName, runDescription)

					case None =>
						interactiveSelect(simulations)
				}

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

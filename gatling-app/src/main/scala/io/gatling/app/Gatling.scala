/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.Try

import com.typesafe.scalalogging.slf4j.StrictLogging

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
  def main(args: Array[String]): Unit = sys.exit(runGatling(args))

  def fromMap(props: mutable.Map[String, Any], simulationClass: Option[Class[Simulation]] = None) = {
    GatlingConfiguration.setUp(props)
    new Gatling(simulationClass).start
  }

  // FIXME : needed for Maven plugin's in-process mode, better be cleaned up
  def runGatling(args: Array[String]): Int = runGatling(args, None)

  def runGatling(args: Array[String], simulationClass: Option[Class[Simulation]]): Int = {
    val props = new GatlingPropertiesBuilder

    val cliOptsParser = new OptionParser[Unit]("gatling") {
      help(Help.full).abbr(Help.abbr).text("Show help (this message) and exit")
      opt[Unit](NoReports.full).abbr(NoReports.abbr).foreach(_ => props.noReports()).text("Runs simulation but does not generate reports")
      opt[Unit](Mute.full).abbr(Mute.abbr).foreach(_ => props.mute()).text("Runs in mute mode: don't asks for run description nor simulation ID, use defaults").hidden()
      opt[String](ReportsOnly.full).abbr(ReportsOnly.abbr).foreach(props.reportsOnly).valueName("<directoryName>").text("Generates the reports for the simulation in <directoryName>")
      opt[String](DataFolder.full).abbr(DataFolder.abbr).foreach(props.dataDirectory).valueName("<directoryPath>").text("Uses <directoryPath> as the absolute path of the directory where feeders are stored")
      opt[String](ResultsFolder.full).abbr(ResultsFolder.abbr).foreach(props.resultsDirectory).valueName("<directoryPath>").text("Uses <directoryPath> as the absolute path of the directory where results are stored")
      opt[String](RequestBodiesFolder.full).abbr(RequestBodiesFolder.abbr).foreach(props.requestBodiesDirectory).valueName("<directoryPath>").text("Uses <directoryPath> as the absolute path of the directory where request bodies are stored")
      opt[String](SimulationsFolder.full).abbr(SimulationsFolder.abbr).foreach(props.sourcesDirectory).valueName("<directoryPath>").text("Uses <directoryPath> to discover simulations that could be run")
      opt[String](SimulationsBinariesFolder.full).abbr(SimulationsBinariesFolder.abbr).foreach(props.binariesDirectory).valueName("<directoryPath>").text("Uses <directoryPath> to discover already compiled simulations")
      opt[String](Simulation.full).abbr(Simulation.abbr).foreach(props.simulationClass).valueName("<className>").text("Runs <className> simulation")
      opt[String](OutputDirectoryBaseName.full).abbr(OutputDirectoryBaseName.abbr).foreach(props.outputDirectoryBaseName).valueName("<name>").text("Use <name> for the base name of the output directory")
      opt[String](SimulationDescription.full).abbr(SimulationDescription.abbr).foreach(props.runDescription).valueName("<description>").text("A short <description> of the run to include in the report")
    }

    // if arguments are incorrect, usage message is displayed
    if (cliOptsParser.parse(args)) fromMap(props.build, simulationClass)
    else GatlingStatusCodes.InvalidArguments
  }
}

class Gatling(simulationClass: Option[Class[Simulation]]) extends StrictLogging {

  def start = {

      def defaultOutputDirectoryBaseName(clazz: Class[Simulation]) =
        configuration.core.outputDirectoryBaseName.getOrElse(clazz.getSimpleName.clean)

      def getSingleSimulation(simulations: List[Class[Simulation]]) =
        configuration.core.simulationClass.map(_ => simulations.head.newInstance)

      def interactiveSelect(simulations: List[Class[Simulation]]): Selection = {

          @tailrec
          def selectSimulationClass(simulations: List[Class[Simulation]]): Class[Simulation] = {

              def readSimulationNumber: Int =
                Try(Console.readInt()).getOrElse {
                  println("Invalid characters, please provide a correct simulation number:")
                  readSimulationNumber
                }

            val selection = simulations.size match {
              case 0 =>
                // If there is no simulation file
                println("There is no simulation script. Please check that your scripts are in user-files/simulations")
                sys.exit()
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

        val userInput: String = {
            @tailrec
            def _userInput: String = {
              println(s"Select simulation id (default is '$myDefaultOutputDirectoryBaseName'). Accepted characters are a-z, A-Z, 0-9, - and _")
              val input = Console.readLine().trim
              if (input.matches("[\\w-_]*"))
                input
              else {
                println(s"$input contains illegal characters")
                _userInput
              }
            }
          _userInput
        }

        val simulationId = if (!userInput.isEmpty) userInput else myDefaultOutputDirectoryBaseName

        println("Select run description (optional)")
        val runDescription = Console.readLine().trim

        new Selection(simulation, simulationId, runDescription)
      }

      def applyAssertions(simulation: Simulation, dataReader: DataReader) = {
        val successful = Assertion.assertThat(simulation.assertions, dataReader)

        if (successful) {
          println("Simulation successful.")
          GatlingStatusCodes.Success
        } else {
          println("Simulation failed.")
          GatlingStatusCodes.AssertionsFailed
        }
      }

      def generateReports(outputDirectoryName: String, dataReader: => DataReader) {
        println("Generating reports...")
        val start = currentTimeMillis
        val indexFile = ReportsGenerator.generateFor(outputDirectoryName, dataReader)
        println(s"Reports generated in ${(currentTimeMillis - start) / 1000}s.")
        println(s"Please open the following file: $indexFile")
      }

    val simulations = simulationClass match {
      case Some(clazz) => List(clazz)
      case None =>
        if (configuration.core.disableCompiler) {
          configuration.core.simulationClass match {
            case Some(className) =>
              List(Class.forName(className).asInstanceOf[Class[Simulation]])

            case None =>
              GatlingFiles.binariesDirectory match {
                case Some(binDir) =>
                  val simulationClassLoader = SimulationClassLoader.fromClasspathBinariesDirectory(binDir)

                  simulationClassLoader
                    .simulationClasses(None)
                    .sortBy(_.getName)

                case None =>
                  throw new IllegalArgumentException("Compiler is disable, but no simulation class or binary directory is specified")
              }
          }

        } else {
          val simulationClassLoader = SimulationClassLoader.fromSourcesDirectory(GatlingFiles.sourcesDirectory)

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
            if (configuration.core.muteMode)
              simulationClass match {
                case Some(clazz) =>
                  Selection(clazz, defaultOutputDirectoryBaseName(clazz), "")
                case None =>
                  throw new UnsupportedOperationException("Mute mode is currently uses by Gatling SBT plugin only.")
              }
            else
              interactiveSelect(simulations)
        }

        val (runId, simulation) = new Runner(selection).run
        (runId, Some(simulation))
    }

    lazy val dataReader = DataReader.newInstance(outputDirectoryName)

    val result = simulation match {
      case Some(s) if !s.assertions.isEmpty => applyAssertions(s, dataReader)
      case _                                => GatlingStatusCodes.Success
    }

    if (!configuration.charting.noReports) generateReports(outputDirectoryName, dataReader)

    result
  }
}

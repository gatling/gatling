/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.app

import java.lang.System.currentTimeMillis

import scala.Console.err
import scala.annotation.tailrec
import scala.io.StdIn
import scala.util.{ Success, Try }

import io.gatling.app.classloader.SimulationClassLoader
import io.gatling.app.cli.ArgsParser
import io.gatling.charts.report.{ ReportsGenerationInputs, ReportsGenerator }
import io.gatling.core.assertion.{ AssertionResult, AssertionValidator }
import io.gatling.core.cli.StatusCode
import io.gatling.core.config.GatlingFiles
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.reader.DataReader
import io.gatling.core.runner.{ Runner, RunResult, Selection }
import io.gatling.core.scenario.Simulation
import io.gatling.core.util.{ Ga, StringHelper }
import io.gatling.core.util.StringHelper.RichString

/**
 * Object containing entry point of application
 */
object Gatling {

  def main(args: Array[String]): Unit = sys.exit(fromArgs(args, None))

  def fromMap(overrides: ConfigOverrides): Int = new Gatling(overrides, None).start.code

  def fromArgs(args: Array[String], simulationClass: SelectedSingleSimulation): Int = {
    val argsParser = new ArgsParser(args)

    argsParser.parseArguments match {
      case Left(commandLineOverrides) =>
        new Gatling(commandLineOverrides, simulationClass).start.code
      case Right(statusCode) => statusCode.code
    }
  }
}
private[app] class Gatling(overrides: ConfigOverrides, simulationClass: SelectedSingleSimulation) {

  def start: StatusCode = {
    StringHelper.checkSupportedJavaVersion()
    implicit val configuration = GatlingConfiguration.load(overrides)
    // ugly way to pass the configuration to the Simulation constructor
    io.gatling.core.Predef.configuration = configuration

    new ConfiguredGatling(simulationClass).start
  }
}

private[app] class ConfiguredGatling(simulationClass: SelectedSingleSimulation)(implicit configuration: GatlingConfiguration) {

  def start: StatusCode = {
    val simulations = loadSimulations
    val singleSimulation = selectSingleSimulationIfPossible(simulations)

    val runResult = runSimulationIfNecessary(singleSimulation, simulations)

    val start = currentTimeMillis

    val dataReader = initDataReaderIfNecessary(runResult)

    dataReader.map { reader =>
      val assertionResults = new AssertionValidator().validateAssertions(reader)
      val reportsGenerationInputs = ReportsGenerationInputs(runResult.runId, reader, assertionResults)

      if (reportsGenerationEnabled) generateReports(reportsGenerationInputs, start)

      runStatus(assertionResults)
    }.getOrElse(GatlingStatusCodes.Success)
  }

  private def loadSimulations: AllSimulations = {
    val fromSbt = simulationClass.isDefined
    val reportsOnly = configuration.core.directory.reportsOnly.isDefined

    if (fromSbt || reportsOnly) Nil
    else SimulationClassLoader(GatlingFiles.binariesDirectory).simulationClasses.sortBy(_.getName)
  }

  private def selectSingleSimulationIfPossible(simulations: AllSimulations): SelectedSingleSimulation = {

      def findSelectedSingleSimulationAmongstCompileOnes(className: String): SelectedSingleSimulation =
        simulations.find(_.getCanonicalName == className)

      def findSelectedSingleSimulationInClassload(className: String): SelectedSingleSimulation =
        Try(Class.forName(className)).toOption.collect { case clazz if classOf[Simulation].isAssignableFrom(clazz) => clazz.asInstanceOf[Class[Simulation]] }

      def singleSimulationFromConfig =
        configuration.core.simulationClass flatMap { className =>
          val found = findSelectedSingleSimulationAmongstCompileOnes(className).orElse(findSelectedSingleSimulationInClassload(className))

          if (found.isEmpty)
            err.println(s"The requested class('$className') can not be found in the classpath or does not extends Simulation.")

          found
        }

      def singleSimulationFromList = simulations match {
        case simulation :: Nil =>
          println(s"${simulation.getName} is the only simulation, executing it.")
          Some(simulation)

        case _ => None
      }

    simulationClass orElse singleSimulationFromConfig orElse singleSimulationFromList
  }

  private def runSimulationIfNecessary(singleSimulation: SelectedSingleSimulation, simulations: AllSimulations): RunResult = {
    configuration.core.directory.reportsOnly.map(RunResult(_, hasAssertions = true)).getOrElse {
      // -- If no single simulation was available, allow user to select one -- //
      val simulation = singleSimulation.getOrElse(interactiveSelect(simulations))

      // -- Ask for simulation ID and run description if required -- //
      val muteModeActive = configuration.core.muteMode || configuration.core.simulationClass.isDefined
      val defaultBaseName = defaultOutputDirectoryBaseName(simulation)
      val optionalDescription = configuration.core.runDescription

      val simulationId = if (muteModeActive) defaultBaseName else askSimulationId(simulation, defaultBaseName)
      val runDescription = optionalDescription.getOrElse(if (muteModeActive) "" else askRunDescription())

      // -- Run Gatling -- //
      val selection = Selection(simulation, simulationId, runDescription)
      Ga.send(configuration)
      new Runner(selection).run
    }
  }

  private def askSimulationId(clazz: Class[Simulation], defaultBaseName: String): String = {
      @tailrec
      def loop(): String = {
        println(s"Select simulation id (default is '$defaultBaseName'). Accepted characters are a-z, A-Z, 0-9, - and _")
        val input = StdIn.readLine().trim
        if (input.matches("[\\w-_]*")) input
        else {
          println(s"$input contains illegal characters")
          loop()
        }
      }

    val input = loop()
    if (input.nonEmpty) input else defaultBaseName
  }

  private def askRunDescription(): String = {
    println("Select run description (optional)")
    StdIn.readLine().trim
  }

  private def interactiveSelect(simulations: AllSimulations): Class[Simulation] = {
    val validRange = 0 until simulations.size

      @tailrec
      def readSimulationNumber: Int = {
        println("Choose a simulation number:")
        for ((simulation, index) <- simulations.zipWithIndex) {
          println(s"     [$index] ${simulation.getName}")
        }

        Try(StdIn.readInt()) match {
          case Success(number) =>
            if (validRange contains number) number
            else {
              println(s"Invalid selection, must be in $validRange")
              readSimulationNumber
            }
          case _ =>
            println("Invalid characters, please provide a correct simulation number:")
            readSimulationNumber
        }
      }

    if (simulations.isEmpty) {
      println("There is no simulation script. Please check that your scripts are in user-files/simulations")
      sys.exit()
    }
    simulations(readSimulationNumber)
  }

  private def initDataReaderIfNecessary(runResult: RunResult): Option[DataReader] = {
    val shouldInitDataReader = reportsGenerationEnabled || runResult.hasAssertions

    if (shouldInitDataReader)
      Some(DataReader.newInstance(runResult.runId))
    else
      None
  }

  private def reportsGenerationEnabled =
    configuration.data.fileDataWriterEnabled && !configuration.charting.noReports

  private def generateReports(reportsGenerationInputs: ReportsGenerationInputs, start: Long): Unit = {
    println("Generating reports...")
    val indexFile = new ReportsGenerator().generateFor(reportsGenerationInputs)
    println(s"Reports generated in ${(currentTimeMillis - start) / 1000}s.")
    println(s"Please open the following file: ${indexFile.toFile}")
  }

  private def runStatus(assertionResults: List[AssertionResult]): StatusCode = {
    val consolidatedAssertionResult = assertionResults.foldLeft(true) { (isValid, assertionResult) =>
      println(s"${assertionResult.message} : ${assertionResult.result}")
      isValid && assertionResult.result
    }

    if (consolidatedAssertionResult) GatlingStatusCodes.Success
    else GatlingStatusCodes.AssertionsFailed
  }

  private def defaultOutputDirectoryBaseName(clazz: Class[Simulation]) =
    configuration.core.outputDirectoryBaseName.getOrElse(clazz.getSimpleName.clean)
}

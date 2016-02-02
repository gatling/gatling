/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import scala.Console._
import scala.annotation.tailrec
import scala.io.StdIn
import scala.util.{ Success, Try }

import io.gatling.app.classloader.SimulationClassLoader
import io.gatling.commons.util.StringHelper._
import io.gatling.core.config.{ GatlingFiles, GatlingConfiguration }
import io.gatling.core.scenario.Simulation

case class Selection(simulationClass: Class[Simulation], userDefinedSimulationId: Option[String], defaultSimulationId: String, description: String)

object Selection {

  def apply(selectedSimulationClass: SelectedSimulationClass, configuration: GatlingConfiguration): Selection =
    new Selector(selectedSimulationClass, configuration).selection

  private class Selector(selectedSimulationClass: SelectedSimulationClass, configuration: GatlingConfiguration) {

    def selection = {

      val simulations = loadSimulations
      val singleSimulation = trySelectingSingleSimulation(simulations)

      // -- If no single simulation was available, allow user to select one -- //
      val simulation = singleSimulation.getOrElse(interactiveSelect(simulations))

      // -- Ask for simulation ID and run description if required -- //
      val muteModeActive = configuration.core.muteMode || configuration.core.simulationClass.isDefined || selectedSimulationClass.isDefined
      val defaultSimulationId = defaultOutputDirectoryBaseName(simulation)
      val optionalDescription = configuration.core.runDescription

      val simulationId = if (muteModeActive) None else askSimulationId(simulation, defaultSimulationId)
      val runDescription = optionalDescription.getOrElse(if (muteModeActive) "" else askRunDescription())

      Selection(simulation, simulationId, defaultSimulationId, runDescription)
    }

    private def loadSimulations: SimulationClasses = {
      val fromSbt = selectedSimulationClass.isDefined
      val reportsOnly = configuration.core.directory.reportsOnly.isDefined

      if (fromSbt || reportsOnly) Nil
      else SimulationClassLoader(GatlingFiles.binariesDirectory(configuration)).simulationClasses.sortBy(_.getName)
    }

    private def trySelectingSingleSimulation(simulationClasses: SimulationClasses): SelectedSimulationClass = {

        def findSelectedSingleSimulationAmongstCompiledOnes(className: String): SelectedSimulationClass =
          simulationClasses.find(_.getCanonicalName == className)

        def findSelectedSingleSimulationInClassloader(className: String): SelectedSimulationClass =
          Try(Class.forName(className)).toOption.collect { case clazz if classOf[Simulation].isAssignableFrom(clazz) => clazz.asInstanceOf[Class[Simulation]] }

        def singleSimulationFromConfig =
          configuration.core.simulationClass.flatMap { className =>
            val found = findSelectedSingleSimulationAmongstCompiledOnes(className).orElse(findSelectedSingleSimulationInClassloader(className))

            if (found.isEmpty)
              err.println(s"The requested class('$className') can not be found in the classpath or does not extends Simulation.")

            found
          }

        def singleSimulationFromList = simulationClasses match {
          case simulation :: Nil =>
            println(s"${simulation.getName} is the only simulation, executing it.")
            Some(simulation)

          case _ => None
        }

      selectedSimulationClass orElse singleSimulationFromConfig orElse singleSimulationFromList
    }

    private def interactiveSelect(simulationClasses: SimulationClasses): Class[Simulation] = {
      val validRange = simulationClasses.indices

        @tailrec
        def readSimulationNumber: Int = {
          println("Choose a simulation number:")
          for ((simulation, index) <- simulationClasses.zipWithIndex) {
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

      if (simulationClasses.isEmpty) {
        println("There is no simulation script. Please check that your scripts are in user-files/simulations")
        sys.exit()
      }
      simulationClasses(readSimulationNumber)
    }

    private def askSimulationId(clazz: Class[Simulation], defaultSimulationId: String): Option[String] = {
        @tailrec
        def loop(): String = {
          println(s"Select simulation id (default is '$defaultSimulationId'). Accepted characters are a-z, A-Z, 0-9, - and _")
          val input = StdIn.readLine().trim
          if (input.matches("[\\w-_]*")) input
          else {
            println(s"$input contains illegal characters")
            loop()
          }
        }

      val input = loop()
      if (input.nonEmpty) Some(input) else None
    }

    private def askRunDescription(): String = {
      println("Select run description (optional)")
      StdIn.readLine().trim
    }

    private def defaultOutputDirectoryBaseName(clazz: Class[Simulation]) =
      configuration.core.outputDirectoryBaseName.getOrElse(clazz.getSimpleName.clean)
  }
}

/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
import scala.util.{ Failure, Success, Try }

import io.gatling.app.classloader.SimulationClassLoader
import io.gatling.commons.util.StringHelper._
import io.gatling.core.config.{ GatlingConfiguration, GatlingFiles }
import io.gatling.core.scenario.Simulation

final class Selection(val simulationClass: Class[Simulation], val simulationId: String, val description: String)

object Selection {

  private val MaxReadSimulationNumberAttempts = 10

  def apply(selectedSimulationClass: SelectedSimulationClass, configuration: GatlingConfiguration): Selection =
    new Selector(selectedSimulationClass).selection(configuration)

  private class Selector(selectedSimulationClass: SelectedSimulationClass) {

    def selection(configuration: GatlingConfiguration): Selection = {
      val userDefinedSimulationClass = configuration.core.simulationClass

      val simulation: Class[Simulation] =
        selectedSimulationClass.getOrElse {
          val simulationClasses: SimulationClasses =
            if (configuration.core.directory.reportsOnly.isDefined) {
              Nil
            } else {
              SimulationClassLoader(GatlingFiles.binariesDirectory(configuration)).simulationClasses.sortBy(_.getName)
            }

          singleSimulationFromConfig(simulationClasses, userDefinedSimulationClass)
            .orElse(singleSimulationFromList(simulationClasses))
            .getOrElse(interactiveSelect(simulationClasses))
        }

      // ask for simulation ID and run description if required
      val simulationId = defaultOutputDirectoryBaseName(simulation, configuration)
      val runDescription =
        configuration.core.runDescription.getOrElse(if (userDefinedSimulationClass.isDefined || selectedSimulationClass.isDefined) "" else askRunDescription())

      new Selection(simulation, simulationId, runDescription)
    }

    private def singleSimulationFromConfig(simulationClasses: SimulationClasses, userDefinedSimulationClass: Option[String]): SelectedSimulationClass = {

      def findUserDefinedSimulationAmongstCompiledOnes(className: String): SelectedSimulationClass =
        simulationClasses.find(_.getCanonicalName == className)

      def findUserDefinedSimulationInClassloader(className: String): SelectedSimulationClass =
        Try(Class.forName(className)) match {
          case Success(clazz) =>
            if (classOf[Simulation].isAssignableFrom(clazz)) {
              Some(clazz.asInstanceOf[Class[Simulation]])
            } else {
              throw new IllegalArgumentException(s"User defined Simulation class $className does not extend of Simulation")
            }

          case Failure(t) =>
            throw new IllegalArgumentException(s"User defined Simulation class $className could not be loaded", t)
        }

      userDefinedSimulationClass.flatMap { userDefinedSimulationClassName =>
        findUserDefinedSimulationAmongstCompiledOnes(userDefinedSimulationClassName)
          .orElse(findUserDefinedSimulationInClassloader(userDefinedSimulationClassName))
      }
    }

    private def singleSimulationFromList(simulationClasses: SimulationClasses) = simulationClasses match {
      case List(simulation) =>
        println(s"${simulation.getName} is the only simulation, executing it.")
        Some(simulation)

      case _ => None
    }

    private def interactiveSelect(simulationClasses: SimulationClasses): Class[Simulation] = {
      val validRange = simulationClasses.indices

      @tailrec
      def readSimulationNumber(attempts: Int): Int = {
        if (attempts > MaxReadSimulationNumberAttempts) {
          println(s"Max attempts of reading simulation number ($MaxReadSimulationNumberAttempts) reached. Aborting.")
          sys.exit(1)
        } else {
          println("Choose a simulation number:")
          for ((simulation, index) <- simulationClasses.zipWithIndex) {
            println(s"     [$index] ${simulation.getName}")
          }

          Try(StdIn.readInt()) match {
            case Success(number) =>
              if (validRange contains number) number
              else {
                println(s"Invalid selection, must be in $validRange")
                readSimulationNumber(attempts + 1)
              }
            case _ =>
              println("Invalid characters, please provide a correct simulation number:")
              readSimulationNumber(attempts + 1)
          }
        }
      }

      if (simulationClasses.isEmpty) {
        println("There is no simulation script. Please check that your scripts are in user-files/simulations")
        sys.exit(1)
      }
      simulationClasses(readSimulationNumber(0))
    }

    private def askRunDescription(): String = {
      println("Select run description (optional)")
      StdIn.readLine().trim
    }

    private def defaultOutputDirectoryBaseName(clazz: Class[Simulation], configuration: GatlingConfiguration) =
      configuration.core.outputDirectoryBaseName.getOrElse(clazz.getSimpleName.clean)
  }
}

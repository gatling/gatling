/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

final class Selection(val simulationClass: SimulationClass, val simulationId: String, val description: String)

object Selection {
  private val MaxReadSimulationNumberAttempts = 10

  def apply(forcedSimulationClass: Option[SimulationClass], configuration: GatlingConfiguration): Selection =
    new Selector(forcedSimulationClass).selection(configuration)

  private class Selector(forcedSimulationClass: Option[SimulationClass]) {
    def selection(configuration: GatlingConfiguration): Selection = {
      val configDefinedSimulationClassName = configuration.core.simulationClass

      val simulation: SimulationClass =
        forcedSimulationClass.getOrElse {
          val simulationClasses: List[SimulationClass] =
            if (configuration.core.directory.reportsOnly.isDefined) {
              Nil
            } else {
              SimulationClassLoader(GatlingFiles.binariesDirectory(configuration.core.directory)).simulationClasses.sortBy(_.canonicalName)
            }

          singleSimulationFromConfig(simulationClasses, configDefinedSimulationClassName)
            .orElse(singleSimulationFromList(simulationClasses))
            .getOrElse(interactiveSelect(simulationClasses))
        }

      // ask for simulation ID and run description if required
      val simulationId = defaultOutputDirectoryBaseName(simulation, configuration)
      val runDescription =
        configuration.core.runDescription.getOrElse(
          if (configDefinedSimulationClassName.isDefined || forcedSimulationClass.isDefined) "" else askRunDescription()
        )

      new Selection(simulation, simulationId, runDescription)
    }

    private def singleSimulationFromConfig(
        simulationClasses: List[SimulationClass],
        configDefinedSimulationClassName: Option[String]
    ): Option[SimulationClass] = {
      def findUserDefinedSimulationAmongstCompiledOnes(className: String): Option[SimulationClass] =
        simulationClasses.find(_.canonicalName == className)

      def findUserDefinedSimulationInClassloader(className: String): Option[SimulationClass] =
        Try(Class.forName(className)) match {
          case Success(clazz) =>
            if (classOf[Simulation].isAssignableFrom(clazz)) {
              Some(SimulationClass.Scala(clazz.asInstanceOf[Class[Simulation]]))
            } else if (classOf[JavaSimulation].isAssignableFrom(clazz)) {
              Some(SimulationClass.Java(clazz.asInstanceOf[Class[JavaSimulation]]))
            } else {
              throw new IllegalArgumentException(s"User defined Simulation class $className does not extend of Simulation")
            }

          case Failure(t) =>
            throw new IllegalArgumentException(s"User defined Simulation class $className could not be loaded", t)
        }

      configDefinedSimulationClassName.flatMap { cn =>
        findUserDefinedSimulationAmongstCompiledOnes(cn).orElse(findUserDefinedSimulationInClassloader(cn))
      }
    }

    private def singleSimulationFromList(simulationClasses: List[SimulationClass]) =
      simulationClasses match {
        case List(simulationClass) =>
          println(s"${simulationClass.canonicalName} is the only simulation, executing it.")
          Some(simulationClass)

        case _ => None
      }

    @SuppressWarnings(Array("org.wartremover.warts.SeqApply"))
    private def interactiveSelect(simulationClasses: List[SimulationClass]): SimulationClass = {
      val validRange = simulationClasses.indices

      @tailrec
      def readSimulationNumber(attempts: Int): Int =
        if (attempts > MaxReadSimulationNumberAttempts) {
          println(s"Max attempts of reading simulation number ($MaxReadSimulationNumberAttempts) reached. Aborting.")
          System.out.flush()
          sys.exit(1)
        } else {
          println("Choose a simulation number:")
          for ((simulation, index) <- simulationClasses.zipWithIndex) {
            println(s"     [$index] ${simulation.canonicalName}")
          }

          Try(StdIn.readInt()) match {
            case Success(number) =>
              if (validRange.contains(number)) {
                number
              } else {
                println(s"Invalid selection, must be in [0, ${simulationClasses.length - 1}].")
                readSimulationNumber(attempts + 1)
              }
            case _ =>
              println("Invalid characters, please provide a correct simulation number:")
              readSimulationNumber(attempts + 1)
          }
        }

      if (simulationClasses.isEmpty) {
        println("Couldn't find any Simulation class. Please check that your Simulations are in the correct location.")
        System.out.flush()
        sys.exit(1)
      }
      simulationClasses(readSimulationNumber(0))
    }

    private def askRunDescription(): String = {
      println("Select run description (optional)")
      StdIn.readLine().trim
    }

    private def defaultOutputDirectoryBaseName(clazz: SimulationClass, configuration: GatlingConfiguration) =
      configuration.core.outputDirectoryBaseName.getOrElse(clazz.simpleName.clean)
  }
}

/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.bundle.commands

import java.io.File
import java.util.UUID

import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Using }

import io.gatling.app.cli.CommandLineConstants.{ Simulation => SimulationOption }
import io.gatling.bundle.{ CommandArguments, EnterpriseBundlePlugin }
import io.gatling.bundle.CommandLineConstants.{ SimulationId, TeamId }
import io.gatling.bundle.commands.CommandHelper._
import io.gatling.bundle.utils.SimulationFilesUtil
import io.gatling.plugin.exceptions.SeveralTeamsFoundException
import io.gatling.plugin.model.Simulation

class EnterpriseRunCommand(config: CommandArguments, args: List[String]) {
  val groupId = "gatling"
  val artifactId = "bundle"

  private[bundle] def run(): Unit = {

    val packagedFile = new PackageCommand(args).run()

    val simulationStartResult = config.simulationId match {
      case Some(simulationId)    => startNonInteractive(simulationId, packagedFile)
      case _ if config.batchMode => createNonInteractive(packagedFile)
      case _                     => createOrStartInteractive(packagedFile)
    }

    simulationStartResult match {
      case Success(simulationResponse) =>
        println(s"""
                   |Simulation ${simulationResponse.simulation.name} successfully started
                   |Once running, reports will be available at: ${config.url.toExternalForm + simulationResponse.runSummary.reportsPath}
                   |""".stripMargin)
      case Failure(e) => throw e
    }

  }

  private def startNonInteractive(simulationId: UUID, file: File) =
    Using(EnterpriseBundlePlugin.getEnterprisePlugin(config)) { enterprisePlugin =>
      enterprisePlugin.uploadPackageAndStartSimulation(simulationId, config.simulationSystemProperties.asJava, file)
    }

  private def createNonInteractive(file: File) = {
    val classes = SimulationFilesUtil.simulationClasses(gatlingHome)
    val chosenSimulation = config.simulationClass.getOrElse(classes.headOption.orNull)

    if (config.simulationId.isEmpty && chosenSimulation == null) {
      throw new IllegalArgumentException(
        s"""
           |Specify --${SimulationId.full} if you want to start a simulation on Gatling Enterprise,
           |or --${SimulationOption.full} if you want to create a new simulation on Gatling Enterprise.
           |See https://gatling.io/docs/gatling/reference/current/core/configuration/#cli-options/ for more information.
           |""".stripMargin
      )
    }

    println("No simulationId configured, creating a new simulation in batch mode")
    Using(EnterpriseBundlePlugin.getEnterprisePlugin(config)) { enterprisePlugin =>
      try {
        val simulationStartResult = enterprisePlugin.createAndStartSimulation(
          config.teamId.orNull,
          groupId,
          artifactId,
          chosenSimulation,
          config.packageId.orNull,
          config.simulationSystemProperties.asJava,
          file
        )
        println(getLogCreatedSimulation(simulationStartResult.simulation, create = true))
        simulationStartResult
      } catch {
        case e: SeveralTeamsFoundException =>
          val teams = e.getAvailableTeams.asScala
          val msg = s"""More than 1 team were found while creating a simulation.
                       |Available teams:
                       |${teams.map(team => s"- ${team.id} (${team.name})").mkString(System.lineSeparator)}
                       |Specify the team you want to use with --${TeamId.full} <teamId>
                       |""".stripMargin
          throw new IllegalArgumentException(msg)
      }
    }
  }

  private def createOrStartInteractive(file: File) =
    Using(EnterpriseBundlePlugin.getEnterpriseInteractivePlugin(config)) { enterprisePlugin =>
      val classes = SimulationFilesUtil.simulationClasses(gatlingHome)

      val simulationStartResult = enterprisePlugin.createOrStartSimulation(
        config.teamId.orNull,
        groupId,
        artifactId,
        config.simulationClass.orNull,
        classes.asJava,
        config.packageId.orNull,
        config.simulationSystemProperties.asJava,
        file
      )
      println(getLogCreatedSimulation(simulationStartResult.simulation, simulationStartResult.createdSimulation))
      simulationStartResult
    }

  private def getLogCreatedSimulation(simulation: Simulation, create: Boolean) = {
    val verb = if (create) {
      "Created"
    } else {
      "Started"
    }
    s"""
       |$verb simulation ${simulation.name} with ID ${simulation.id}
       |
       |To start directly the same simulation, please add this option to your command:
       |--${SimulationId.full} ${simulation.id}
       |
       |See https://gatling.io/docs/gatling/reference/current/core/configuration/#cli-options/ for more information.
       |""".stripMargin
  }
}

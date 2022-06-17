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

import java.util.UUID

import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Try, Using }

import io.gatling.app.cli.CommandLineConstants.{ Simulation => SimulationOption }
import io.gatling.bundle.{ BundleIO, CommandArguments, EnterpriseBundlePlugin }
import io.gatling.bundle.CommandLineConstants.{ SimulationId, TeamId }
import io.gatling.plugin.EnterprisePlugin
import io.gatling.plugin.client.EnterpriseClient
import io.gatling.plugin.exceptions._
import io.gatling.plugin.model.Simulation

object EnterpriseRunCommand {
  val GroupId = "gatling"
  val ArtifactId = "bundle"
}

class EnterpriseRunCommand(config: CommandArguments, args: List[String]) {

  import EnterpriseRunCommand._

  private val logger = BundleIO.getLogger

  private[bundle] def run(): Unit =
    Try {
      val enterpriseClient: EnterpriseClient = EnterpriseBundlePlugin.getClient(config)
      val enterprisePlugin: EnterprisePlugin =
        if (config.batchMode) EnterpriseBundlePlugin.getBatchEnterprisePlugin(enterpriseClient)
        else EnterpriseBundlePlugin.getInteractiveEnterprisePlugin(enterpriseClient)

      val serverInformation = enterpriseClient.getServerInformation
      val maxJavaVersion = serverInformation.versions.java.max.toInt

      val file = new PackageCommand(config, args, maxJavaVersion).run()

      val simulationStartResult = config.simulationId match {
        case Some(simulationId) =>
          enterprisePlugin.uploadPackageAndStartSimulation(
            simulationId,
            config.simulationSystemProperties.asJava,
            config.simulationEnvironmentVariables.asJava,
            config.simulationClass.orNull,
            file
          )
        case _ =>
          enterprisePlugin.createAndStartSimulation(
            config.teamId.orNull,
            GroupId,
            ArtifactId,
            config.simulationClass.orNull,
            config.packageId.orNull,
            config.simulationSystemProperties.asJava,
            config.simulationEnvironmentVariables.asJava,
            file
          )
      }

      if (simulationStartResult.createdSimulation) {
        logCreatedSimulation(simulationStartResult.simulation)
      }

      if (config.simulationId.isEmpty) {
        logSimulationConfiguration(simulationStartResult.simulation.id)
      }

      val reportsUrl = config.url.toExternalForm + simulationStartResult.runSummary.reportsPath
      logger.info(s"Simulation successfully started; once running, reports will be available at $reportsUrl")
    }
      .recoverWith {
        case e: UnsupportedJavaVersionException =>
          Failure(new IllegalArgumentException(s"""${e.getMessage}
                                                  |In order to target the supported Java bytecode version, please use Java JDK ${e.supportedVersion}.
                                                  |Or, reported class may come from your project dependencies, published targeting Java ${e.version}.
                                                  |""".stripMargin))
        case e: SeveralTeamsFoundException =>
          val teams = e.getAvailableTeams.asScala
          Failure(new IllegalArgumentException(s"""More than 1 team were found while creating a simulation.
                                                  |Available teams:
                                                  |${teams.map(team => s"- ${team.id} (${team.name})").mkString(System.lineSeparator)}
                                                  |Specify the team you want to use with --${TeamId.full} ${teams.head.id}
                                                  |""".stripMargin))
        case e: SeveralSimulationClassNamesFoundException =>
          val simulationClasses = e.getAvailableSimulationClassNames.asScala
          Failure(new IllegalArgumentException(s"""Several simulation classes were found
                                                  |${simulationClasses.map("- " + _).mkString("\n")}
                                                  |Specify the team you want to use with --${SimulationOption.full} ${simulationClasses.head}
                                                  |""".stripMargin))
        case e: SimulationStartException =>
          if (e.isCreated) {
            logCreatedSimulation(e.getSimulation)
          }
          logSimulationConfiguration(e.getSimulation.id)
          Failure(e.getCause)
      }
      .fold(throw _, _ => ())

  private def logCreatedSimulation(simulation: Simulation): Unit =
    logger.info(s"Created simulation named ${simulation.name} with ID '${simulation.id}'")

  private def logSimulationConfiguration(simulationId: UUID): Unit =
    logger.info(s"""
                   |Specify --${SimulationId.full} $simulationId if you want to start a simulation on Gatling Enterprise,
                   |or --${SimulationOption.full} $simulationId if you want to create a new simulation on Gatling Enterprise.
                   |See https://gatling.io/docs/gatling/reference/current/core/configuration/#cli-options/ for more information.
                   |""".stripMargin)
}

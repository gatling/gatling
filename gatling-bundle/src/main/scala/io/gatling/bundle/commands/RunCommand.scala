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

package io.gatling.bundle.commands

import scala.jdk.CollectionConverters._

import io.gatling.bundle.{ BundleIO, CommandArguments }
import io.gatling.bundle.CommandArguments.RunMode
import io.gatling.bundle.CommandLineConstants.{ RunMode => RunModeOption }
import io.gatling.plugin.io.input.InputChoice

private[bundle] final class RunCommand(config: CommandArguments, args: List[String], displayHelp: () => Unit) {
  private[bundle] def run(): Unit =
    config.runMode match {
      case Some(runMode) =>
        runMode match {
          case RunMode.RunLocal          => new OpenSourceRunCommand(config, args).run()
          case RunMode.RunEnterprise     => new EnterpriseRunCommand(config, args).run()
          case RunMode.EnterprisePackage => new EnterprisePackageCommand(config, args, cleanFile = false).run()
        }
      case _ =>
        if (config.simulationId.nonEmpty) {
          println("Running the Simulation on Gatling Enterprise")
          new EnterpriseRunCommand(config, args).run()
        } else if (config.reportsOnly.nonEmpty) {
          println("Generating Gatling reports locally")
          new OpenSourceRunCommand(config, args).run()
        } else if (config.batchMode) {
          throw new IllegalArgumentException(
            s"""
               |If you're running Gatling in batch mode, you need to set the runMode option:
               |- '--${RunModeOption.full} ${RunMode.RunLocal.value}' if you want to start the Simulation locally
               |- '--${RunModeOption.full} ${RunMode.RunEnterprise.value}' if you want to upload the Simulation to Gatling Enterprise Cloud, and run it there
               |- '--${RunModeOption.full} ${RunMode.EnterprisePackage.value}' if you want to package the Simulation for Gatling Enterprise
               |""".stripMargin
          )
        } else {
          println("Do you want to run the simulation locally, on Gatling Enterprise, or just package it?")
          val inputChoice = new InputChoice(BundleIO)
          val RunGatlingOpenSource = "Run the Simulation locally"
          val RunGatlingEnterprise = "Package and upload the Simulation to Gatling Enterprise Cloud, and run it there"
          val RunPackage = "Package the Simulation for Gatling Enterprise"
          val RunHelp = "Show help and exit"
          val choice = inputChoice.inputFromStringList(List(RunGatlingOpenSource, RunGatlingEnterprise, RunPackage, RunHelp).asJava, false)

          choice match {
            case RunGatlingOpenSource => new OpenSourceRunCommand(config, args).run()
            case RunPackage           => new EnterprisePackageCommand(config, args, cleanFile = false).run()
            case RunGatlingEnterprise => new EnterpriseRunCommand(config, args).run()
            case RunHelp              => displayHelp()
            case _                    => throw new IllegalArgumentException(s"Couldn't recognize the chosen option $choice")
          }
        }
    }
}

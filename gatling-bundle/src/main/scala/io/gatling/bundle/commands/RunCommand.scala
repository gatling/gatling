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

import scala.jdk.CollectionConverters._

import io.gatling.bundle.{ BundleIO, CommandArguments }
import io.gatling.bundle.CommandArguments.{ RunEnterprise, RunLocal }
import io.gatling.bundle.CommandLineConstants.{ RunMode => RunModeOption }
import io.gatling.plugin.io.input.InputChoice

class RunCommand(config: CommandArguments, args: List[String]) {
  private[bundle] def run(): Unit = {
    config.runMode match {
      case Some(runMode) =>
        runMode match {
          case RunLocal      => new OpenSourceRunCommand(args).run()
          case RunEnterprise => new EnterpriseRunCommand(config, args).run()
        }
      case _ =>
        if (config.simulationId.nonEmpty) {
          println("Running the Simulation on Gatling Enterprise")
          new EnterpriseRunCommand(config, args).run()
        } else if (config.reportsOnly.nonEmpty) {
          println("Running the Simulation locally")
          new OpenSourceRunCommand(args).run()
        } else if (config.batchMode) {
          throw new IllegalArgumentException(s"""
                                                |If you're running Gatling in batch mode, you need to set the runMode option:
                                                |- '--${RunModeOption.full} ${RunLocal.value}' if you want to start the Simulation locally
                                                |- '--${RunModeOption.full} ${RunEnterprise.value}' if you want to start the Simulation on Gatling Enterprise
                                                |""".stripMargin)
        } else {
          println("Do you want to run the simulation locally or Enterprise?")
          val inputChoice = new InputChoice(BundleIO)
          val runGatlingOpenSource = "Run the Simulation locally"
          val runGatlingEnterprise = "Run the Simulation on Gatling Enterprise"
          val isOpenSource = inputChoice.inputFromStringList(List(runGatlingOpenSource, runGatlingEnterprise).asJava, false).equals(runGatlingOpenSource)

          if (isOpenSource) {
            new OpenSourceRunCommand(args).run()
          } else {
            new EnterpriseRunCommand(config, args).run()
          }
        }
    }
  }
}

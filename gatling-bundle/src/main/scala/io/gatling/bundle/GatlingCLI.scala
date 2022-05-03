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

package io.gatling.bundle

import java.net.URL
import java.util.{ Locale, UUID }

import io.gatling.app.cli.CommandLineConstants._
import io.gatling.bundle.CommandArguments.{ RunEnterprise, RunLocal, RunMode }
import io.gatling.bundle.CommandLineConstants.{ RunMode => RunModeOption, _ }
import io.gatling.bundle.commands.RunCommand
import io.gatling.core.cli.GatlingOptionParser
import io.gatling.plugin.exceptions.UserQuitException
import io.gatling.recorder.cli.CommandLineConstants.SimulationsFolder

import scopt.Read

object GatlingCLI {
  def main(args: Array[String]): Unit = {
    implicit val uuidRead: Read[UUID] = Read.reads(UUID.fromString)

    implicit val runModeRead: Read[RunMode] = Read.reads(input =>
      input.toLowerCase(Locale.ROOT) match {
        case RunLocal.value      => RunLocal
        case RunEnterprise.value => RunEnterprise
        case _ =>
          throw new IllegalArgumentException(
            s"""
               |Please specify:
               |'--${RunModeOption.full} ${RunLocal.value}' to start the Simulation locally or
               |'--${RunModeOption.full} ${RunEnterprise.value}' to start the Simulation on Gatling Enterprise""".stripMargin
          )
      }
    )

    val parser = new GatlingOptionParser[CommandArguments]("gatling") {

      help(Help)

      opt[RunMode](RunModeOption).action((x, c) => c.copy(runMode = Some(x)))

      note("")
      note("Following options are used when compiling your Gatling simulations")
      opt[String](SimulationsFolder)
      opt[String](BinariesFolder)
      opt[String](ExtraScalacOptions)

      note("")
      note("Following options are used when running Gatling locally")
      opt[Unit](NoReports)
      opt[String](ReportsOnly).action((x, c) => c.copy(reportsOnly = Some(x)))
      opt[String](ResourcesFolder)
      opt[String](ResultsFolder)
      opt[String](BinariesFolder)
      opt[String](Simulation).action((x, c) => c.copy(simulationClass = Some(x)))
      opt[String](RunDescription)

      note("")
      note("Following options are used when running Gatling on Gatling Enterprise")
      opt[Unit](BatchMode).action((_, c) => c.copy(batchMode = true))
      opt[String](ApiToken).action((x, c) => c.copy(apiToken = Some(x)))
      opt[UUID](SimulationId).action((x, c) => c.copy(simulationId = Some(x)))
      opt[UUID](PackageId).action((x, c) => c.copy(packageId = Some(x)))
      opt[UUID](TeamId).action((x, c) => c.copy(teamId = Some(x)))
      opt[String](Simulation)
      opt[URL](Url).action((x, c) => c.copy(url = x)).hidden()
      opt[Map[String, String]](SimulationSystemProperties).action((x, c) => c.copy(simulationSystemProperties = x))
    }

    parser.parse(args, CommandArguments.Empty) match {
      case Some(config) =>
        try {
          new RunCommand(config, args.toList).run()
        } catch {
          case e: UserQuitException =>
            println(e.getMessage)
            System.out.flush()
            sys.exit(0)
        }
      case _ =>
    }
  }
}

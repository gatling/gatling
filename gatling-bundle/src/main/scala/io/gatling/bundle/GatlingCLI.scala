/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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
import java.nio.file.Paths
import java.util.{ Locale, UUID }

import io.gatling.app.cli.CommandLineConstants._
import io.gatling.bundle.CommandArguments.RunMode
import io.gatling.bundle.CommandLineConstants.{ RunMode => RunModeOption, _ }
import io.gatling.bundle.commands.RunCommand
import io.gatling.core.cli.GatlingOptionParser
import io.gatling.plugin.exceptions.UserQuitException
import io.gatling.plugin.util.Fork
import io.gatling.recorder.cli.CommandLineConstants.SimulationsFolder

import scopt.Read

object GatlingCLI {
  def main(args: Array[String]): Unit = {
    implicit val uuidRead: Read[UUID] = Read.reads(UUID.fromString)

    implicit val runModeRead: Read[RunMode] = Read.reads(input =>
      input.toLowerCase(Locale.ROOT) match {
        case RunMode.RunLocal.value          => RunMode.RunLocal
        case RunMode.RunEnterprise.value     => RunMode.RunEnterprise
        case RunMode.EnterprisePackage.value => RunMode.EnterprisePackage
        case _ =>
          throw new IllegalArgumentException(
            s"""
               |Please specify:
               |'--${RunModeOption.full} ${RunMode.RunLocal.value}' to start the Simulation locally,
               |'--${RunModeOption.full} ${RunMode.RunEnterprise.value}' to start the Simulation on Gatling Enterprise Cloud or
               |'--${RunModeOption.full} ${RunMode.EnterprisePackage.value}' to package the Simulation for Gatling Enterprise""".stripMargin
          )
      }
    )

    val parser = new GatlingOptionParser[CommandArguments]("gatling") {
      help(Help)

      opt[RunMode](RunModeOption).action((x, c) => c.copy(runMode = Some(x)))

      note("")
      note("Generic options:")
      opt[String](SimulationsFolder)
      opt[String](BinariesFolder).action((x, c) => c.copy(binariesDirectory = Paths.get(x).toAbsolutePath))
      opt[String](ResourcesFolder).action((x, c) => c.copy(resourcesDirectory = Paths.get(x).toAbsolutePath))
      opt[String](ExtraScalacOptions)
      opt[String](Simulation).action((x, c) => c.copy(simulationClass = Some(x)))
      opt[String](ExtraCompilerJvmOptions).action((x, c) => c.copy(extraJavaOptionsCompile = whitespaceSeparatedList(x)))

      note("")
      note("Options specific to running locally:")
      opt[Unit](NoReports)
      opt[String](ReportsOnly).action((x, c) => c.copy(reportsOnly = Some(x)))
      opt[String](ResultsFolder)
      opt[String](RunDescription)
      opt[String](ExtraRunJvmOptions).action((x, c) => c.copy(extraJavaOptionsRun = whitespaceSeparatedList(x)))

      note("")
      note("Options specific to running on Gatling Cloud:")
      opt[Unit](BatchMode).action((_, c) => c.copy(batchMode = true))
      opt[Unit](WaitForRunEnd).action((_, c) => c.copy(waitForRunEnd = true))
      opt[String](ApiToken).action((x, c) => c.copy(apiToken = Some(x)))
      opt[UUID](SimulationId).action((x, c) => c.copy(simulationId = Some(x)))
      opt[UUID](PackageId).action((x, c) => c.copy(packageId = Some(x)))
      opt[UUID](TeamId).action((x, c) => c.copy(teamId = Some(x)))
      opt[URL](Url).action((x, c) => c.copy(url = x)).hidden()
      opt[URL](controlPlaneUrl).action((x, c) => c.copy(controlPlaneUrl = Some(x)))
      opt[Map[String, String]](SimulationSystemProperties).action((x, c) => c.copy(simulationSystemProperties = x))
      opt[Map[String, String]](SimulationEnvironmentVariables).action((x, c) => c.copy(simulationEnvironmentVariables = x))
    }

    val displayHelp = () => {
      parser.parse(List(s"-${Help.abbr}"), CommandArguments.Empty)
      ()
    }

    parser.parse(args, CommandArguments.Empty) match {
      case Some(config) =>
        try {
          new RunCommand(config, args.toList, displayHelp).run()
        } catch {
          case e: UserQuitException =>
            exitWithoutStacktrace(e, 0)
          case e: Fork.ForkException =>
            exitWithoutStacktrace(e, e.exitValue)
          case RunFailedException =>
            exitWithoutStacktrace(RunFailedException, 1)
        }
      case _ =>
    }
  }

  private def exitWithoutStacktrace(e: Exception, exitValue: Int): Unit = {
    Option(e.getMessage).foreach(println)
    System.out.flush()
    sys.exit(exitValue)
  }

  private def whitespaceSeparatedList(x: String): List[String] =
    x.split("""\s+""").filter(_.nonEmpty).toList
}

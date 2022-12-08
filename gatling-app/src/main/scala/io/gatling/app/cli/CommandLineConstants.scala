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

package io.gatling.app.cli

import io.gatling.core.cli.CommandLineConstant

private[gatling] object CommandLineConstants {
  val NoReports = new CommandLineConstant("no-reports", "nr", "Runs simulation but does not generate reports", None)
  val ReportsOnly = new CommandLineConstant("reports-only", "ro", "Generates the reports for the simulation in <directoryName>", Some("<directoryName>"))
  val ResultsFolder = new CommandLineConstant(
    "results-folder",
    "rf",
    "Uses <directoryPath> as the absolute path of the directory where results are stored",
    Some("<directoryPath>")
  )
  val ResourcesFolder = new CommandLineConstant(
    "resources-folder",
    "rsf",
    "Uses <directoryPath> as the absolute path of the directory where resources are stored",
    Some("<directoryPath>")
  )
  val BinariesFolder = new CommandLineConstant(
    "binaries-folder",
    "bf",
    "Uses <directoryPath> as the absolute path of the directory where binaries (jar files) are stored",
    Some("<directoryPath>")
  )
  val Simulation = new CommandLineConstant("simulation", "s", "Runs <className> simulation", Some("<className>"))
  val RunDescription = new CommandLineConstant("run-description", "rd", "A short <description> of the run to include in the report", Some("<description>"))
  val Launcher = new CommandLineConstant("launcher", "l", "The program that launched Gatling", Some(""))
  val BuildToolVersion = new CommandLineConstant("build-tool-version", "btv", "The version of the build tool used to launch Gatling", Some(""))

  val AllOptions: List[CommandLineConstant] =
    List(NoReports, ReportsOnly, ResultsFolder, ResourcesFolder, BinariesFolder, Simulation, RunDescription, Launcher, BuildToolVersion)
}

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

package io.gatling.app.cli

import io.gatling.core.cli.CliOption

private[gatling] object GatlingOptions {
  val NoReports: CliOption = new CliOption("no-reports", "nr", "Runs simulation but does not generate reports", None)
  val ReportsOnly: CliOption = new CliOption("reports-only", "ro", "Generates the reports for the simulation in <directoryName>", Some("<directoryName>"))
  val ResultsFolder: CliOption = new CliOption(
    "results-folder",
    "rf",
    "Uses <directoryPath> as the absolute path of the directory where results are stored",
    Some("<directoryPath>")
  )
  val Simulation: CliOption = new CliOption("simulation", "s", "Runs <className> simulation", Some("<className>"))
  val RunDescription: CliOption = new CliOption("run-description", "rd", "A short <description> of the run to include in the report", Some("<description>"))
  val Launcher: CliOption = new CliOption("launcher", "l", "The program that launched Gatling", Some(""))
  val BuildToolVersion: CliOption = new CliOption("build-tool-version", "btv", "The version of the build tool used to launch Gatling", Some(""))
}

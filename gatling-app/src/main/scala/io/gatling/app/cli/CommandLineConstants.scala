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

package io.gatling.app.cli

import io.gatling.core.cli.CommandLineConstant

private object CommandLineConstants {
  val Help = new CommandLineConstant("help", "h")
  val NoReports = new CommandLineConstant("no-reports", "nr")
  val ReportsOnly = new CommandLineConstant("reports-only", "ro")
  val ResultsFolder = new CommandLineConstant("results-folder", "rf")
  val ResourcesFolder = new CommandLineConstant("resources-folder", "rsf")
  val SimulationsFolder = new CommandLineConstant("simulations-folder", "sf")
  val BinariesFolder = new CommandLineConstant("binaries-folder", "bf")
  val Simulation = new CommandLineConstant("simulation", "s")
  val RunDescription = new CommandLineConstant("run-description", "rd")
}

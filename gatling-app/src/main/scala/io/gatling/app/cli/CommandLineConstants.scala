/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

private[cli] object CommandLineConstants {
  val Help = CommandLineConstant("help", "h")
  val NoReports = CommandLineConstant("no-reports", "nr")
  val ReportsOnly = CommandLineConstant("reports-only", "ro")
  val ResultsFolder = CommandLineConstant("results-folder", "rf")
  val ResourcesFolder = CommandLineConstant("resources-folder", "rsf")
  val SimulationsFolder = CommandLineConstant("simulations-folder", "sf")
  val BinariesFolder = CommandLineConstant("binaries-folder", "bf")
  val Simulation = CommandLineConstant("simulation", "s")
  val RunDescription = CommandLineConstant("run-description", "rd")
}

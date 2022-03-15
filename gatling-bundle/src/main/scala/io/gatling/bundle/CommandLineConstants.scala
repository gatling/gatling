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

import io.gatling.core.cli.CommandLineConstant

private[bundle] object CommandLineConstants {

  val RunMode = new CommandLineConstant(
    "run-mode",
    "rm",
    "Specify if you want to run the Simulation with the Open-source or Enterprise version. Options are 'opensource' and 'enterprise'",
    None
  )
  val BatchMode = new CommandLineConstant("batch-mode", "bm", "No interactive user input will be asked", None)
  val ApiToken = new CommandLineConstant("api-token", "at", "Gatling Enterprise's API token with the 'Configure' role", None)
  val PackageId = new CommandLineConstant("package-id", "pid", "Specifies the Gatling Enterprise Package, when creating a new Simulation", None)
  val SimulationId = new CommandLineConstant("simulation-id", "sid", "Specifies the Gatling Enterprise Simulation that needs to be started", None)
  val TeamId = new CommandLineConstant("team-id", "tid", "Specifies the Gatling Enterprise Team, when creating a new Simulation", None)
  val Url = new CommandLineConstant("url", "url", "Overrides cloud.gatling.io when connecting to Gatling Enterprise", None)
  val SimulationSystemProperties =
    new CommandLineConstant(
      "simulation-system-properties",
      "ssp",
      "Optional System Properties used when starting the Gatling Enterprise simulation",
      Some("k1=v1,k2=v2")
    )
  val ExtraScalacOptions: CommandLineConstant =
    new CommandLineConstant("extra-scalac-options", "eso", "Defines additional scalac options for the compiler", None)
}

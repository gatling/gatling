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

import io.gatling.bundle.CommandArguments.{ RunEnterprise, RunLocal, RunPackage }
import io.gatling.core.cli.CommandLineConstant

private[bundle] object CommandLineConstants {
  val Help = new CommandLineConstant("help", "h", "Show help (this message) and exit", None)
  val RunMode = new CommandLineConstant(
    "run-mode",
    "rm",
    s"Specify if you want to run the Simulation locally, on Gatling Enterprise or package the simulation. Options are '${RunLocal.value}', '${RunEnterprise.value}' and '${RunPackage.value}'",
    Some(RunLocal.value)
  )
  val BatchMode = new CommandLineConstant("batch-mode", "bm", "No interactive user input will be asked", None)
  val ApiToken = new CommandLineConstant("api-token", "at", "Gatling Enterprise's API token with the 'Configure' role", None)
  val PackageId = new CommandLineConstant("package-id", "pid", "Specifies the Gatling Enterprise Package, when creating a new Simulation", None)
  val SimulationId = new CommandLineConstant("simulation-id", "sid", "Specifies the Gatling Enterprise Simulation that needs to be started", None)
  val TeamId = new CommandLineConstant("team-id", "tid", "Specifies the Gatling Enterprise Team, when creating a new Simulation", None)
  val Url = new CommandLineConstant("url", "url", "Overrides https://cloud.gatling.io when connecting to Gatling Enterprise", None)
  val SimulationSystemProperties =
    new CommandLineConstant(
      "simulation-system-properties",
      "ssp",
      "Optional System Properties used when starting the Gatling Enterprise simulation",
      Some("k1=v1,k2=v2")
    )
  val SimulationEnvironmentVariables =
    new CommandLineConstant(
      "simulation-environment-variables",
      "sev",
      "Optional Environment Variables used when starting the Gatling Enterprise simulation",
      Some("k1=v1,k2=v2")
    )
  val ExtraCompilerJvmOptions: CommandLineConstant = new CommandLineConstant(
    "extra-compiler-jvm-options",
    "ecjo",
    """Defines additional JVM options used when compiling your code (e.g. setting the heap size with "-Xms2G -Xmx4G"). See https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html for available options.""",
    Some(""""-Option1 -Option2"""")
  )
  val ExtraJavacOptions: CommandLineConstant = new CommandLineConstant(
    "extra-javac-options",
    "ejo",
    "Defines additional compiler options for your Java code. See https://download.java.net/java/early_access/panama/docs/specs/man/javac.html#options for available options.",
    Some(""""-Option1 -Option2"""")
  )
  val ExtraScalacOptions: CommandLineConstant = new CommandLineConstant(
    "extra-scalac-options",
    "eso",
    "Defines additional compiler options for your Scala code. See https://docs.scala-lang.org/overviews/compiler-options/index.html for available options.)",
    Some(""""-Option1 -Option2"""")
  )
  val ExtraRunJvmOptions: CommandLineConstant = new CommandLineConstant(
    "extra-run-jvm-options",
    "erjo",
    """Defines additional JVM options used when running your code locally (e.g. setting the heap size with "-Xms2G -Xmx4G"). See https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html for available options.""",
    Some(""""-Option1 -Option2"""")
  )
}

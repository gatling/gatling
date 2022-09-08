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

import io.gatling.app.cli.CommandLineConstants._
import io.gatling.bundle.{ BundleIO, CLIHelper, CommandArguments }
import io.gatling.bundle.commands.CommandHelper._
import io.gatling.plugin.GatlingConstants
import io.gatling.plugin.util.{ Fork, JavaLocator }

private[bundle] final class OpenSourceRunCommand(config: CommandArguments, args: List[String]) {
  private[bundle] def run(): Unit = {
    Compiler.compile(config, args, maxJavaVersion = None)

    val classPath = gatlingLibs ++ userLibs ++ userResources ++ gatlingConfFiles

    // Note: options which come later in the list can override earlier ones (because the java command will use the last
    // occurrence in its arguments list in case of conflict)
    val runJavaOptions = GatlingConstants.DEFAULT_JVM_OPTIONS_GATLING.asScala ++ systemJavaOpts ++ config.extraJavaOptionsRun

    new Fork(
      "io.gatling.app.Gatling",
      classPath.asJava,
      runJavaOptions.asJava,
      (CLIHelper.filterArgOptions(
        args,
        AllOptions
      ) ::: List("-l", "bundle")).asJava,
      JavaLocator.getJavaExecutable,
      true,
      BundleIO.getLogger,
      GatlingHome.toFile
    ).run()
  }
}

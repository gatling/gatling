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
import io.gatling.bundle.CommandLineConstants._
import io.gatling.bundle.commands.CommandHelper._
import io.gatling.plugin.GatlingConstants
import io.gatling.plugin.util.{ Fork, JavaLocator }
import io.gatling.recorder.cli.CommandLineConstants._

private[commands] object Compiler {

  private val CompilerMemoryOptions = List("-Xmx1G", "-Xss100M")

  def compile(config: CommandArguments, args: List[String], maxJavaVersion: Option[Int]): Unit = {
    // Note: options which come later in the list can override earlier ones (because the java command will use the last
    // occurrence in its arguments list in case of conflict)
    val compilerJavaOptions = GatlingConstants.DEFAULT_JVM_OPTIONS_BASE.asScala ++ CompilerMemoryOptions ++ systemJavaOpts ++ config.extraJavaOptionsCompile

    val classPath = gatlingLibs ++ userLibs ++ gatlingConfFiles

    val extraJavacOptions = maxJavaVersion match {
      case Some(maxVersion) if GatlingConstants.JAVA_MAJOR_VERSION > maxVersion =>
        println(
          s"Currently running on unsupported Java version ${GatlingConstants.JAVA_MAJOR_VERSION}; Java code will be compiled with the '--release $maxVersion' option"
        )
        List(s"--${ExtraJavacOptions.full}", s"--release,$maxVersion")
      case _ =>
        Nil
    }

    val extraScalacOptions = optionListEnv("EXTRA_SCALAC_OPTIONS")
      .flatMap(options => List(s"--${ExtraScalacOptions.full}", options))

    new Fork(
      "io.gatling.compiler.ZincCompiler",
      classPath.asJava,
      compilerJavaOptions.asJava,
      (extraJavacOptions ++ extraScalacOptions ++ CLIHelper.filterArgOptions(args, List(SimulationsFolder, BinariesFolder, ExtraScalacOptions))).asJava,
      JavaLocator.getJavaExecutable,
      true,
      BundleIO.getLogger,
      GatlingHome.toFile
    ).run()
  }
}
